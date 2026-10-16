package com.aerolinea.gestionvuelos.service;

import java.math.BigDecimal;

public interface PaymentGateway {

  CheckoutSession createCheckoutSession(PaymentOrderService.PendingOrder order);

  GatewayPayment getCheckoutSessionPayment(String sessionId);

  boolean refund(String paymentIntentId);

  boolean expireCheckoutSession(String sessionId);

  boolean verifyWebhookSignature(String payload, String signatureHeader);

  String getCheckoutSessionId(String payload);

  String getCurrency();

  record CheckoutSession(String id, String checkoutUrl) {}

  record GatewayPayment(
      String id, String status, String externalReference, BigDecimal amount, String currency) {}
}
