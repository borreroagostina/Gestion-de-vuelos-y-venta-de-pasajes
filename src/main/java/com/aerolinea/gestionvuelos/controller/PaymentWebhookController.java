package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.service.PaymentNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos/stripe")
public class PaymentWebhookController {

  private final PaymentNotificationService notificationService;

  public PaymentWebhookController(PaymentNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> webhook(
      @RequestBody String payload,
      @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
    notificationService.processWebhook(payload, signature);
    return ResponseEntity.ok().build();
  }
}
