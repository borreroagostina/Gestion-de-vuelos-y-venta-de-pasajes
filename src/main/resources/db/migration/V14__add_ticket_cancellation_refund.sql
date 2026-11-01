ALTER TABLE reserva_asiento
    ADD COLUMN cancelado_en DATETIME NULL,
    ADD COLUMN monto_reembolso DECIMAL(12, 2) NULL,
    ADD COLUMN estado_reembolso VARCHAR(20) NULL;

CREATE INDEX idx_reserva_asiento_reembolso ON reserva_asiento (estado_reembolso);
