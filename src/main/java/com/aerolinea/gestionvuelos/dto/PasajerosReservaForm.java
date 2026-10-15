package com.aerolinea.gestionvuelos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class PasajerosReservaForm {

  @Valid
  @NotEmpty(message = "Debes completar los datos de los pasajeros")
  private List<PasajeroViajeForm> pasajeros = new ArrayList<>();

  public static PasajerosReservaForm crearParaCantidad(int cantidadAsientos) {
    final PasajerosReservaForm form = new PasajerosReservaForm();
    for (int i = 0; i < cantidadAsientos; i++) {
      form.getPasajeros().add(new PasajeroViajeForm());
    }
    return form;
  }

  public List<PasajeroViajeForm> getPasajeros() {
    return pasajeros;
  }

  public void setPasajeros(List<PasajeroViajeForm> pasajeros) {
    this.pasajeros = pasajeros;
  }
}
