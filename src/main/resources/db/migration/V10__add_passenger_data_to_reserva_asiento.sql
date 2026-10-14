ALTER TABLE reserva_asiento
    ADD COLUMN nombre_pasajero VARCHAR(100) NULL,
    ADD COLUMN apellido_pasajero VARCHAR(100) NULL,
    ADD COLUMN documento_pasajero VARCHAR(20) NULL,
    ADD COLUMN fecha_nacimiento_pasajero DATE NULL;
