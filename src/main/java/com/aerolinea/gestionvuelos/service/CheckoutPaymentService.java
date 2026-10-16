package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.dto.PasajeroViajeForm;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckoutPaymentService {

  private final PaymentOrderService orderService;
  private final PaymentGateway gatewayService;

  public CheckoutPaymentService(PaymentOrderService orderService, PaymentGateway gatewayService) {
    this.orderService = orderService;
    this.gatewayService = gatewayService;
  }

  public String startPayment(Long userId, Long reservationId, List<PasajeroViajeForm> passengers) {
    final PaymentOrderService.PendingOrder order =
        orderService.createOrder(userId, reservationId, passengers, gatewayService.getCurrency());
    try {
      final PaymentGateway.CheckoutSession preference = gatewayService.createCheckoutSession(order);
      orderService.savePreference(order.orderCode(), preference.id(), preference.checkoutUrl());
      return preference.checkoutUrl();
    } catch (RuntimeException exception) {
      orderService.markGatewayError(order.orderCode());
      throw exception;
    }
  }

  public String startPaymentFromSavedPassengerData(Long userId, Long reservationId) {
    final PaymentOrderService.PendingOrder order =
        orderService.createOrderFromSavedPassengerData(
            userId, reservationId, gatewayService.getCurrency());
    try {
      final PaymentGateway.CheckoutSession preference = gatewayService.createCheckoutSession(order);
      orderService.savePreference(order.orderCode(), preference.id(), preference.checkoutUrl());
      return preference.checkoutUrl();
    } catch (RuntimeException exception) {
      orderService.markGatewayError(order.orderCode());
      throw exception;
    }
  }
}
