ALTER TABLE vuelos_programados ADD COLUMN cancelado BOOLEAN DEFAULT FALSE;
ALTER TABLE vuelos_programados ADD COLUMN motivo_cancelacion VARCHAR(255);

ALTER TABLE instancias_vuelo ADD COLUMN cancelada BOOLEAN DEFAULT FALSE;
ALTER TABLE instancias_vuelo ADD COLUMN motivo_cancelacion VARCHAR(255);
