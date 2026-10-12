CREATE TABLE IF NOT EXISTS asientos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    instancia_vuelo_id BIGINT NOT NULL,
    numero VARCHAR(10) NOT NULL,
    clase VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    PRIMARY KEY (id),
    CONSTRAINT fk_asiento_instancia FOREIGN KEY (instancia_vuelo_id) REFERENCES instancias_vuelo(id) ON DELETE CASCADE,
    UNIQUE KEY uk_asiento_instancia_numero (instancia_vuelo_id, numero)
);

CREATE TABLE IF NOT EXISTS reservas_temporales (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reserva_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS reserva_asientos (
    reserva_id BIGINT NOT NULL,
    asiento_id BIGINT NOT NULL,
    PRIMARY KEY (reserva_id, asiento_id),
    CONSTRAINT fk_rel_reserva FOREIGN KEY (reserva_id) REFERENCES reservas_temporales(id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_asiento FOREIGN KEY (asiento_id) REFERENCES asientos(id) ON DELETE CASCADE
);
