ALTER TABLE reservas_temporales
    ADD COLUMN sesion_id VARCHAR(100) NULL;

CREATE TABLE IF NOT EXISTS reserva_asiento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reserva_temporal_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    asiento_id BIGINT NOT NULL,
    sesion_id VARCHAR(100) NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reserva_asiento_reserva FOREIGN KEY (reserva_temporal_id) REFERENCES reservas_temporales(id),
    CONSTRAINT fk_reserva_asiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_reserva_asiento_asiento FOREIGN KEY (asiento_id) REFERENCES asientos(id),
    INDEX idx_reserva_asiento_instancia_estado (asiento_id, estado, fecha_expiracion)
);
