package com.aerolinea.gestionvuelos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class DatosPasajerosForm {

  @Valid
  @NotEmpty(message = "Debe completar al menos un pasajero")
  private List<DatosPasajeroForm> pasajeros = new ArrayList<>();

  public List<DatosPasajeroForm> getPasajeros() {
    return pasajeros;
  }

  public void setPasajeros(List<DatosPasajeroForm> pasajeros) {
    this.pasajeros = pasajeros;
  }
}
