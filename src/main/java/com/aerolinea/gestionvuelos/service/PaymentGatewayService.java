package com.aerolinea.gestionvuelos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class PaymentGatewayService implements PaymentGateway {

  private static final long WEBHOOK_TOLERANCE_SECONDS = 300L;
  private static final long STRIPE_SESSION_EXPIRY_BUFFER_MINUTES = 31L;
  private static final int STRIPE_CURRENCY_SCALE = 2;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final RestClient restClient;
  private final String secretKey;
  private final String webhookSecret;
  private final String baseUrl;
  private final String currency;

  public PaymentGatewayService(
      RestClient.Builder restClientBuilder,
      @Value("${app.payment.stripe.secret-key:}") String secretKey,
      @Value("${app.payment.stripe.webhook-secret:}") String webhookSecret,
      @Value("${app.base-url:http://localhost:8080}") String baseUrl,
      @Value("${app.payment.stripe.api-base-url:https://api.stripe.com}") String apiBaseUrl,
      @Value("${app.payment.currency:ARS}") String currency) {
    this.restClient = restClientBuilder.baseUrl(apiBaseUrl).build();
    this.secretKey = secretKey.trim();
    this.webhookSecret = webhookSecret.trim();
    this.baseUrl = baseUrl.replaceAll("/$", "");
    this.currency = currency.toLowerCase();
  }

  @Override
  public CheckoutSession createCheckoutSession(PaymentOrderService.PendingOrder order) {
    requireCredentials();
    if (!order.reservationExpiry().isAfter(LocalDateTime.now())) {
      throw new IllegalStateException("La reserva temporal expiró antes de iniciar el pago.");
    }

    final MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
    request.add("mode", "payment");
    request.add("success_url", successUrl(order.orderCode()));
    request.add("cancel_url", cancelUrl(order.orderCode()));
    request.add("client_reference_id", order.orderCode());
    request.add("customer_email", order.payerEmail());
    request.add("line_items[0][price_data][currency]", order.currency().toLowerCase());
    request.add(
        "line_items[0][price_data][unit_amount]",
        order
            .amount()
            .movePointRight(STRIPE_CURRENCY_SCALE)
            .setScale(0, RoundingMode.UNNECESSARY)
            .toPlainString());
    request.add(
        "line_items[0][price_data][product_data][name]",
        "Compra de pasajes - " + order.orderCode());
    request.add("line_items[0][quantity]", "1");
    request.add("metadata[order_code]", order.orderCode());
    final long minimumStripeExpiry =
        Instant.now()
            .plus(STRIPE_SESSION_EXPIRY_BUFFER_MINUTES, ChronoUnit.MINUTES)
            .getEpochSecond();
    final long reservationExpiry =
        order.reservationExpiry().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
    request.add("expires_at", Long.toString(Math.max(minimumStripeExpiry, reservationExpiry)));

    final JsonNode response =
        restClient
            .post()
            .uri("/v1/checkout/sessions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(request)
            .retrieve()
            .body(JsonNode.class);
    if (response == null || !response.hasNonNull("id") || !response.hasNonNull("url")) {
      throw new IllegalStateException("Stripe no devolvió una sesión de checkout válida.");
    }
    return new CheckoutSession(response.get("id").asText(), response.get("url").asText());
  }

  @Override
  public GatewayPayment getCheckoutSessionPayment(String sessionId) {
    requireCredentials();
    final JsonNode response =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/v1/checkout/sessions/{id}")
                        .queryParam("expand[]", "payment_intent")
                        .build(sessionId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .retrieve()
            .body(JsonNode.class);
    if (response == null
        || !response.hasNonNull("id")
        || !response.hasNonNull("client_reference_id")
        || !response.hasNonNull("amount_total")
        || !response.hasNonNull("currency")) {
      throw new IllegalStateException("Stripe devolvió una sesión de pago incompleta.");
    }

    final JsonNode paymentIntent = response.path("payment_intent");
    final String transactionId =
        paymentIntent.isObject() && paymentIntent.hasNonNull("id")
            ? paymentIntent.get("id").asText()
            : paymentIntent.isTextual() ? paymentIntent.asText() : response.get("id").asText();
    final String paymentIntentStatus =
        paymentIntent.isObject() ? paymentIntent.path("status").asText("") : "";
    final String status = mapSessionStatus(response, paymentIntentStatus);
    return new GatewayPayment(
        transactionId,
        status,
        response.get("client_reference_id").asText(),
        BigDecimal.valueOf(response.get("amount_total").asLong(), STRIPE_CURRENCY_SCALE),
        response.get("currency").asText().toUpperCase());
  }

  @Override
  public boolean refund(String paymentIntentId) {
    requireCredentials();
    final MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
    request.add("payment_intent", paymentIntentId);
    final JsonNode response =
        restClient
            .post()
            .uri("/v1/refunds")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(request)
            .retrieve()
            .body(JsonNode.class);
    return response != null
        && response.hasNonNull("status")
        && "succeeded".equalsIgnoreCase(response.get("status").asText());
  }

  @Override
  public boolean expireCheckoutSession(String sessionId) {
    requireCredentials();
    final JsonNode response =
        restClient
            .post()
            .uri("/v1/checkout/sessions/{id}/expire", sessionId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .retrieve()
            .body(JsonNode.class);
    return response != null
        && response.hasNonNull("status")
        && "expired".equalsIgnoreCase(response.get("status").asText());
  }

  @Override
  public boolean verifyWebhookSignature(String payload, String signatureHeader) {
    if (webhookSecret.isBlank() || payload == null || signatureHeader == null) {
      return false;
    }
    try {
      final String timestamp = signatureValue(signatureHeader, "t");
      final String receivedSignature = signatureValue(signatureHeader, "v1");
      if (timestamp == null || receivedSignature == null || !timestamp.matches("\\d+")) {
        return false;
      }
      final long timestampSeconds = Long.parseLong(timestamp);
      final long nowSeconds = System.currentTimeMillis() / 1000L;
      if (Math.abs(nowSeconds - timestampSeconds) > WEBHOOK_TOLERANCE_SECONDS) {
        return false;
      }
      final Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      final byte[] expected =
          mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
      return MessageDigest.isEqual(expected, HexFormat.of().parseHex(receivedSignature));
    } catch (IllegalArgumentException | java.security.GeneralSecurityException exception) {
      return false;
    }
  }

  @Override
  public String getCurrency() {
    return currency.toUpperCase();
  }

  @Override
  public String getCheckoutSessionId(String payload) {
    final JsonNode event = parseWebhook(payload);
    final String eventType = event.path("type").asText();
    if (!List.of(
            "checkout.session.completed",
            "checkout.session.async_payment_succeeded",
            "checkout.session.async_payment_failed")
        .contains(eventType)) {
      return null;
    }
    final JsonNode session = event.path("data").path("object");
    return session.path("id").asText(null);
  }

  private String successUrl(String orderCode) {
    return baseUrl
        + "/pasajero/checkout/resultado?orden="
        + orderCode
        + "&session_id={CHECKOUT_SESSION_ID}";
  }

  private String cancelUrl(String orderCode) {
    return baseUrl + "/pasajero/checkout/resultado?orden=" + orderCode + "&cancelled=true";
  }

  private String mapSessionStatus(JsonNode session, String paymentIntentStatus) {
    if ("paid".equalsIgnoreCase(session.path("payment_status").asText())) {
      return "approved";
    }
    if ("expired".equalsIgnoreCase(session.path("status").asText())
        || "requires_payment_method".equalsIgnoreCase(paymentIntentStatus)
        || "canceled".equalsIgnoreCase(paymentIntentStatus)) {
      return "rejected";
    }
    return "pending";
  }

  private JsonNode parseWebhook(String payload) {
    try {
      return OBJECT_MAPPER.readTree(payload);
    } catch (java.io.IOException exception) {
      throw new IllegalArgumentException("El webhook de Stripe contiene JSON inválido.", exception);
    }
  }

  private String signatureValue(String signatureHeader, String key) {
    for (String component : signatureHeader.split(",")) {
      final String[] pair = component.trim().split("=", 2);
      if (pair.length == 2 && key.equals(pair[0])) {
        return pair[1];
      }
    }
    return null;
  }

  private void requireCredentials() {
    if (secretKey.isBlank()) {
      throw new IllegalStateException("Falta configurar STRIPE_SECRET_KEY.");
    }
  }
}
