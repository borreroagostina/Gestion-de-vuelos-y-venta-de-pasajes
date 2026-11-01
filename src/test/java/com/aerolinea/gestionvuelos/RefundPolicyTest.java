package com.aerolinea.gestionvuelos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aerolinea.gestionvuelos.service.RefundPolicy;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RefundPolicyTest {

  @Test
  void refundIsAlwaysFull() {
    assertEquals(
        RefundPolicy.FULL_PERCENT, RefundPolicy.percentage("ECONOMY", Duration.ofDays(10)));
    assertEquals(
        RefundPolicy.FULL_PERCENT, RefundPolicy.percentage("ECONOMY", Duration.ofHours(2)));
    assertEquals(
        RefundPolicy.FULL_PERCENT, RefundPolicy.percentage("PRIMERA", Duration.ofHours(2)));
  }

  @Test
  void refundAmountAppliesPercentage() {
    assertEquals(
        new BigDecimal("100.00"),
        RefundPolicy.refundAmount(new BigDecimal("100.00"), RefundPolicy.FULL_PERCENT));
  }
}
