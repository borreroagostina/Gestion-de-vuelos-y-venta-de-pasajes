package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.aerolinea.gestionvuelos.service.PaymentGateway;
import com.aerolinea.gestionvuelos.service.PaymentGatewayService;
import com.aerolinea.gestionvuelos.service.PaymentOrderService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class PaymentGatewayServiceTest {

  private static final String WEBHOOK_SECRET = "whsec_test_secret";
  private MockRestServiceServer server;
  private PaymentGatewayService gatewayService;

  @Before
  public void setUp() {
    final RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    gatewayService =
        new PaymentGatewayService(
            builder,
            "sk_test_example",
            WEBHOOK_SECRET,
            "http://localhost:8080",
            "https://api.stripe.test",
            "ARS");
  }

  @Test
  public void createsHostedStripeCheckoutSessionWithoutSendingCardData() {
    server
        .expect(requestTo("https://api.stripe.test/v1/checkout/sessions"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(Matchers.containsString("mode=payment")))
        .andExpect(content().string(Matchers.containsString("client_reference_id=order-123")))
        .andExpect(content().string(Matchers.containsString("unit_amount")))
        .andExpect(content().string(Matchers.not(Matchers.containsString("4111111111111111"))))
        .andRespond(
            withSuccess(
                "{\"id\":\"cs_test_123\",\"url\":\"https://checkout.stripe.test/session\"}",
                MediaType.APPLICATION_JSON));

    final PaymentOrderService.PendingOrder order =
        new PaymentOrderService.PendingOrder(
            "order-123",
            new BigDecimal("1210.00"),
            "ARS",
            "passenger@example.test",
            LocalDateTime.now().plusMinutes(8));

    final PaymentGateway.CheckoutSession preference = gatewayService.createCheckoutSession(order);

    assertEquals("cs_test_123", preference.id());
    assertEquals("https://checkout.stripe.test/session", preference.checkoutUrl());
    server.verify();
  }

  @Test
  public void verifiesStripeWebhookSignatureAndExtractsCheckoutSession() throws Exception {
    final String timestamp = Long.toString(System.currentTimeMillis() / 1000);
    final String payload =
        "{\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"id\":\"cs_test_123\"}}}";
    final String signedPayload = timestamp + "." + payload;
    final Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    final String signature =
        "t="
            + timestamp
            + ",v1="
            + HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

    assertTrue(gatewayService.verifyWebhookSignature(payload, signature));
    assertFalse(gatewayService.verifyWebhookSignature(payload + " ", signature));
    assertFalse(gatewayService.verifyWebhookSignature(payload, "t=1,v1=00"));
    assertEquals("cs_test_123", gatewayService.getCheckoutSessionId(payload));
  }

  @Test
  public void fetchesAuthoritativeCheckoutSessionAndPaymentIntentStatus() {
    server
        .expect(
            requestTo(
                Matchers.containsString(
                    "https://api.stripe.test/v1/checkout/sessions/cs_test_123")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"id\":\"cs_test_123\",\"client_reference_id\":\"order-123\","
                    + "\"amount_total\":121000,\"currency\":\"ars\",\"payment_status\":\"paid\","
                    + "\"status\":\"complete\",\"payment_intent\":{\"id\":\"pi_test_123\","
                    + "\"status\":\"succeeded\"}}",
                MediaType.APPLICATION_JSON));

    final PaymentGateway.GatewayPayment payment =
        gatewayService.getCheckoutSessionPayment("cs_test_123");

    assertEquals("pi_test_123", payment.id());
    assertEquals("approved", payment.status());
    assertEquals("order-123", payment.externalReference());
    assertEquals(0, new BigDecimal("1210.00").compareTo(payment.amount()));
    assertEquals("ARS", payment.currency());
    server.verify();
  }
}
