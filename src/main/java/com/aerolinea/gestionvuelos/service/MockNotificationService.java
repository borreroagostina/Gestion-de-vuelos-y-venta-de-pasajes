package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(MockNotificationService.class);

    @Override
    public void notificarCancelacionVuelo(InstanciaVuelo instancia, String motivo) {
        logger.info("[MOCK NOTIFICATION] Notificando a pasajeros del vuelo {} ({}) sobre la cancelacion: {}",
                instancia.getVueloProgramado().getCodigoVuelo(),
                instancia.getFechaSalida(),
                motivo);
        // US-19: Integrar con el sistema real de notificaciones
    }
}
