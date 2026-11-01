package com.aerolinea.gestionvuelos.service;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentGateway {

  CheckoutSession createCheckoutSession(PaymentOrderService.PendingOrder order);

  GatewayPayment getCheckoutSessionPayment(String sessionId);

  boolean refund(String paymentIntentId);

  boolean refund(String paymentIntentId, BigDecimal amount, String idempotencyKey);

  List<BigDecimal> getSucceededRefundAmounts(String paymentIntentId);

  boolean expireCheckoutSession(String sessionId);

  boolean verifyWebhookSignature(String payload, String signatureHeader);

  String getCheckoutSessionId(String payload);

  ConfirmedRefund getConfirmedRefund(String payload);

  String getCurrency();

  record ConfirmedRefund(String paymentIntentId, BigDecimal amount, String currency) {}

  record CheckoutSession(String id, String checkoutUrl) {}

  record GatewayPayment(
      String id, String status, String externalReference, BigDecimal amount, String currency) {}
}
