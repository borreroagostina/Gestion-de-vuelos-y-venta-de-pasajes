package com.aerolinea.gestionvuelos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Reglas de reembolso por cancelacion de pasaje (US-16), alineadas con las condiciones de tarifa
 * mostradas en US-12. Pendientes de confirmacion con el PO (riesgo R5).
 */
public final class RefundPolicy {

  public static final String FIRST_CLASS = "PRIMERA";
  public static final int FULL_PERCENT = 100;
  private static final long HOURS_PER_WEEK = 168L;
  private static final long HOURS_TWO_DAYS = 48L;
  private static final long HOURS_ONE_DAY = 24L;
  private static final int PERCENT_HIGH = 80;
  private static final int PERCENT_MEDIUM = 50;
  private static final int PERCENT_LOW = 25;

  private RefundPolicy() {}

  /**
   * Porcentaje del precio del pasaje que se devuelve segun clase y anticipacion.
   *
   * <p>ECONOMY: 7 dias o mas 80%, 48 hs o mas 50%, menos de 48 hs 25%. PRIMERA: 48 hs o mas 100%,
   * 24 hs o mas 80%, menos de 24 hs 50%.
   */
  public static int percentage(String clase, Duration anticipation) {
    final long hours = anticipation.toHours();
    if (FIRST_CLASS.equalsIgnoreCase(clase)) {
      if (hours >= HOURS_TWO_DAYS) {
        return FULL_PERCENT;
      }
      return hours >= HOURS_ONE_DAY ? PERCENT_HIGH : PERCENT_MEDIUM;
    }
    if (hours >= HOURS_PER_WEEK) {
      return PERCENT_HIGH;
    }
    return hours >= HOURS_TWO_DAYS ? PERCENT_MEDIUM : PERCENT_LOW;
  }

  public static BigDecimal refundAmount(BigDecimal ticketPrice, int percentage) {
    return ticketPrice
        .multiply(BigDecimal.valueOf(percentage))
        .divide(BigDecimal.valueOf(FULL_PERCENT), 2, RoundingMode.HALF_UP);
  }
}
