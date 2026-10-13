package com.aerolinea.gestionvuelos.model;

import java.time.DayOfWeek;

/** Mapeo de dias de la semana para la interfaz en español. */
public enum DiaSemana {
  MONDAY("Lunes", DayOfWeek.MONDAY),
  TUESDAY("Martes", DayOfWeek.TUESDAY),
  WEDNESDAY("Miércoles", DayOfWeek.WEDNESDAY),
  THURSDAY("Jueves", DayOfWeek.THURSDAY),
  FRIDAY("Viernes", DayOfWeek.FRIDAY),
  SATURDAY("Sábado", DayOfWeek.SATURDAY),
  SUNDAY("Domingo", DayOfWeek.SUNDAY);

  private final String etiqueta;
  private final DayOfWeek dayOfWeek;

  DiaSemana(final String etiqueta, final DayOfWeek dayOfWeek) {
    this.etiqueta = etiqueta;
    this.dayOfWeek = dayOfWeek;
  }

  public String getEtiqueta() {
    return etiqueta;
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }
}
