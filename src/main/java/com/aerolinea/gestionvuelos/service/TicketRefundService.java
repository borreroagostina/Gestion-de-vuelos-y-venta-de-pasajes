package com.aerolinea.gestionvuelos.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Orquesta la cancelacion de pasajes (US-16): registra la cancelacion, reembolsa por la pasarela y
 * avisa al pasajero. Si la pasarela no responde, el reembolso queda pendiente y se reintenta.
 */
@Service
public class TicketRefundService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TicketRefundService.class);

  private final TicketCancellationService cancellationService;
  private final PaymentGateway gateway;
  private final TicketMailService mailService;

  public TicketRefundService(
      TicketCancellationService cancellationService,
      PaymentGateway gateway,
      TicketMailService mailService) {
    this.cancellationService = cancellationService;
    this.gateway = gateway;
    this.mailService = mailService;
  }

  /** Cancela el pasaje e intenta el reembolso. Devuelve true si el reembolso ya se confirmó. */
  public CancellationOutcome cancelarPasaje(Long reservaAsientoId, String email) {
    final TicketCancellationService.CancellationResult result =
        cancellationService.cancelar(reservaAsientoId, email);
    final boolean refunded = procesarReembolso(result.ticketId());
    return new CancellationOutcome(result.refundAmount(), result.percentage(), refunded);
  }

  @Scheduled(fixedDelayString = "${app.payment.refund-retry-ms:60000}")
  public void reintentarReembolsosPendientes() {
    for (Long ticketId : cancellationService.idsReembolsosPendientes()) {
      procesarReembolso(ticketId);
    }
  }

  public void confirmarReembolsoExterno(PaymentGateway.ConfirmedRefund refund) {
    final List<TicketCancellationService.RefundRequest> matchingRequests = new ArrayList<>();
    for (TicketCancellationService.RefundRequest request : pendingRequests()) {
      if (request.paymentIntentId().equals(refund.paymentIntentId())
          && request.amount().compareTo(refund.amount()) == 0
          && request.currency().equalsIgnoreCase(refund.currency())) {
        matchingRequests.add(request);
      }
    }
    if (matchingRequests.size() != 1) {
      LOGGER.warn(
          "El reembolso confirmado de Stripe para el pago {} no se pudo asociar de forma "
              + "inequívoca a un pasaje pendiente (coincidencias: {}).",
          refund.paymentIntentId(),
          matchingRequests.size());
      return;
    }

    final TicketCancellationService.RefundRequest request = matchingRequests.get(0);
    if (cancellationService.marcarReembolsado(request.ticketId())) {
      notificar(request);
    }
  }

  private boolean procesarReembolso(Long ticketId) {
    try {
      final var pending = cancellationService.obtenerReembolsoPendiente(ticketId);
      if (pending.isEmpty()) {
        return false;
      }
      final TicketCancellationService.RefundRequest request = pending.get();
      if (yaReembolsadoEnPasarela(request)) {
        if (cancellationService.marcarReembolsado(ticketId)) {
          notificar(request);
        }
        return true;
      }
      final boolean succeeded =
          gateway.refund(
              request.paymentIntentId(), request.amount(), "refund-ticket-" + request.ticketId());
      if (!succeeded) {
        LOGGER.warn("El reembolso del pasaje {} continúa pendiente en la pasarela.", ticketId);
        return false;
      }
      if (cancellationService.marcarReembolsado(ticketId)) {
        notificar(request);
      }
      return true;
    } catch (RuntimeException exception) {
      LOGGER.error(
          "No se pudo reembolsar el pasaje {}. Se reintentará automáticamente.",
          ticketId,
          exception);
      return false;
    }
  }

  /**
   * Detecta un reembolso exitoso ya hecho en Stripe (por ejemplo, manual) solo si es inequívoco: un
   * único reembolso con ese importe y un único pasaje pendiente con ese pago e importe.
   */
  private boolean yaReembolsadoEnPasarela(TicketCancellationService.RefundRequest request) {
    final long succeededWithAmount =
        gateway.getSucceededRefundAmounts(request.paymentIntentId()).stream()
            .filter(amount -> amount.compareTo(request.amount()) == 0)
            .count();
    if (succeededWithAmount != 1) {
      return false;
    }
    final long pendingWithSameKey =
        pendingRequests().stream()
            .filter(
                other ->
                    other.paymentIntentId().equals(request.paymentIntentId())
                        && other.amount().compareTo(request.amount()) == 0)
            .count();
    return pendingWithSameKey == 1;
  }

  private List<TicketCancellationService.RefundRequest> pendingRequests() {
    final List<TicketCancellationService.RefundRequest> requests = new ArrayList<>();
    for (Long id : cancellationService.idsReembolsosPendientes()) {
      try {
        cancellationService.obtenerReembolsoPendiente(id).ifPresent(requests::add);
      } catch (RuntimeException exception) {
        LOGGER.warn("Se omite el pasaje {} al conciliar reembolsos.", id, exception);
      }
    }
    return requests;
  }

  private void notificar(TicketCancellationService.RefundRequest request) {
    try {
      mailService.enviarConfirmacionCancelacion(
          request.email(),
          request.flightCode(),
          request.seatNumber(),
          request.passengerName(),
          request.amount(),
          request.currency());
    } catch (RuntimeException exception) {
      LOGGER.error(
          "No se pudo enviar el email de cancelación del pasaje {}.",
          request.ticketId(),
          exception);
    }
  }

  public record CancellationOutcome(BigDecimal refundAmount, int percentage, boolean refunded) {}
}
