package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.InstanciaVuelo;

public interface NotificationService {
    void notificarCancelacionVuelo(InstanciaVuelo instancia, String motivo);
}
