CREATE TABLE IF NOT EXISTS aeropuertos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo_iata VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    pais VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS aeronaves (
    id BIGINT NOT NULL AUTO_INCREMENT,
    modelo VARCHAR(80) NOT NULL,
    fabricante VARCHAR(80) NOT NULL,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    economy_seats INT NOT NULL,
    primera_clase_seats INT NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE vuelos
    ADD COLUMN origen_aeropuerto_id BIGINT NULL,
    ADD COLUMN destino_aeropuerto_id BIGINT NULL,
    ADD COLUMN aeronave_id BIGINT NULL,
    ADD CONSTRAINT fk_vuelo_origen_aeropuerto
        FOREIGN KEY (origen_aeropuerto_id) REFERENCES aeropuertos(id),
    ADD CONSTRAINT fk_vuelo_destino_aeropuerto
        FOREIGN KEY (destino_aeropuerto_id) REFERENCES aeropuertos(id),
    ADD CONSTRAINT fk_vuelo_aeronave
        FOREIGN KEY (aeronave_id) REFERENCES aeronaves(id);
