package com.aerolinea.gestionvuelos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aerolinea.gestionvuelos.service.RefundPolicy;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RefundPolicyTest {

  private static final int HIGH = 80;
  private static final int MEDIUM = 50;
  private static final int LOW = 25;
  private static final long HOURS_THREE_DAYS = 72L;
  private static final long HOURS_TWO_DAYS = 48L;
  private static final long HOURS_ONE_AND_A_HALF_DAYS = 30L;

  @Test
  void economyRefundDependsOnAnticipation() {
    assertEquals(HIGH, RefundPolicy.percentage("ECONOMY", Duration.ofDays(10)));
    assertEquals(MEDIUM, RefundPolicy.percentage("ECONOMY", Duration.ofHours(HOURS_THREE_DAYS)));
    assertEquals(LOW, RefundPolicy.percentage("ECONOMY", Duration.ofHours(5)));
  }

  @Test
  void firstClassRefundDependsOnAnticipation() {
    assertEquals(
        RefundPolicy.FULL_PERCENT,
        RefundPolicy.percentage("PRIMERA", Duration.ofHours(HOURS_TWO_DAYS)));
    assertEquals(
        HIGH, RefundPolicy.percentage("PRIMERA", Duration.ofHours(HOURS_ONE_AND_A_HALF_DAYS)));
    assertEquals(MEDIUM, RefundPolicy.percentage("PRIMERA", Duration.ofHours(2)));
  }

  @Test
  void refundAmountAppliesPercentage() {
    assertEquals(
        new BigDecimal("80.00"), RefundPolicy.refundAmount(new BigDecimal("100.00"), HIGH));
  }
}
