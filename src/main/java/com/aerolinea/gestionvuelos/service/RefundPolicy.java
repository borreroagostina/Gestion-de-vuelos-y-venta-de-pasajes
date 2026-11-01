package com.aerolinea.gestionvuelos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Reglas de reembolso por cancelacion de pasaje (US-16). Siempre que la cancelacion este permitida
 * (antes de la salida del vuelo) se reembolsa el 100% del precio del pasaje.
 */
public final class RefundPolicy {

  public static final String FIRST_CLASS = "PRIMERA";
  public static final int FULL_PERCENT = 100;

  private RefundPolicy() {}

  /** Porcentaje del precio del pasaje que se devuelve: siempre el 100%. */
  public static int percentage(String clase, Duration anticipation) {
    return FULL_PERCENT;
  }

  public static BigDecimal refundAmount(BigDecimal ticketPrice, int percentage) {
    return ticketPrice
        .multiply(BigDecimal.valueOf(percentage))
        .divide(BigDecimal.valueOf(FULL_PERCENT), 2, RoundingMode.HALF_UP);
  }
}
