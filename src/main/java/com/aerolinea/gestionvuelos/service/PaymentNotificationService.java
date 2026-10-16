package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentNotificationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PaymentNotificationService.class);
  private final PaymentGateway gatewayService;
  private final PaymentOrderService orderService;

  public PaymentNotificationService(
      PaymentGateway gatewayService, PaymentOrderService orderService) {
    this.gatewayService = gatewayService;
    this.orderService = orderService;
  }

  public void processWebhook(String payload, String signature) {
    if (!gatewayService.verifyWebhookSignature(payload, signature)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firma de Stripe inválida.");
    }
    final String sessionId = gatewayService.getCheckoutSessionId(payload);
    if (sessionId != null) {
      processVerifiedPayment(sessionId, null);
    }
  }

  public void refreshFromGateway(String paymentId, String expectedOrderCode) {
    processVerifiedPayment(paymentId, expectedOrderCode);
  }

  public void cancelCheckoutSession(String orderCode, String email) {
    final PaymentOrder order = orderService.findOwnedOrder(orderCode, email);
    if (order.getStatus() != PaymentOrderStatus.PENDING
        || order.getPreferenceId() == null
        || order.getPreferenceId().isBlank()) {
      return;
    }
    try {
      if (gatewayService.expireCheckoutSession(order.getPreferenceId())) {
        orderService.rejectPendingOrder(orderCode, email);
      } else {
        LOGGER.warn("Stripe no confirmó la cancelación de la orden {}.", orderCode);
      }
    } catch (RestClientException exception) {
      LOGGER.error("No se pudo cancelar la sesión Stripe de la orden {}.", orderCode, exception);
    }
  }

  private void processVerifiedPayment(String paymentId, String expectedOrderCode) {
    final PaymentGateway.GatewayPayment payment =
        gatewayService.getCheckoutSessionPayment(paymentId);
    if (expectedOrderCode != null && !expectedOrderCode.equals(payment.externalReference())) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "El pago no corresponde a esta orden.");
    }
    final PaymentOrderStatus status = orderService.applyPayment(payment);
    if (status == PaymentOrderStatus.REFUND_PENDING) {
      refund(payment.id());
    }
  }

  @Scheduled(fixedDelayString = "${app.payment.refund-retry-ms:60000}")
  public void retryLatePaymentRefunds() {
    final List<PaymentOrder> pending = orderService.findRefundPending();
    for (PaymentOrder order : pending) {
      refund(order.getExternalTransactionId());
    }
  }

  @Scheduled(fixedDelayString = "${app.payment.order-expiry-poll-ms:30000}")
  public void expireCheckoutSessionsForReleasedReservations() {
    for (String sessionId : orderService.findExpiredPendingCheckoutSessionIds()) {
      try {
        if (gatewayService.expireCheckoutSession(sessionId)) {
          orderService.markExpiredByCheckoutSessionId(sessionId);
        } else {
          LOGGER.warn("Stripe no confirmó el vencimiento de la sesión {}.", sessionId);
        }
      } catch (RestClientException exception) {
        LOGGER.error(
            "No se pudo cancelar la sesión Stripe {} al vencer la reserva.", sessionId, exception);
      }
    }
  }

  private void refund(String paymentId) {
    if (paymentId == null || paymentId.isBlank()) {
      LOGGER.error("No se puede reembolsar un pago aprobado tardío sin ID externo.");
      return;
    }
    try {
      if (gatewayService.refund(paymentId)) {
        orderService.markRefunded(paymentId);
      } else {
        LOGGER.warn(
            "El reembolso del pago tardío {} continúa pendiente en la pasarela.", paymentId);
      }
    } catch (RuntimeException exception) {
      LOGGER.error(
          "No se pudo devolver el pago tardío {}. Se reintentará automáticamente.",
          paymentId,
          exception);
    }
  }
}
