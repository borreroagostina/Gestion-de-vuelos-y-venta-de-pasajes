package com.aerolinea.gestionvuelos.model;

public enum MetodoPagoMostrador {
  EFECTIVO("Efectivo"),
  TARJETA_POS("Tarjeta (POS)");

  private final String etiqueta;

  MetodoPagoMostrador(String etiqueta) {
    this.etiqueta = etiqueta;
  }

  public String getEtiqueta() {
    return etiqueta;
  }
}
