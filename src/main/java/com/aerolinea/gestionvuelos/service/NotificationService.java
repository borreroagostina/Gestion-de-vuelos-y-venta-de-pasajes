package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import java.time.LocalTime;

public interface NotificationService {
  void notificarCancelacionVuelo(InstanciaVuelo instancia, String motivo);

  void notificarCambioHorario(
      InstanciaVuelo instancia, LocalTime horaSalidaAnterior, LocalTime horaLlegadaAnterior);
}
