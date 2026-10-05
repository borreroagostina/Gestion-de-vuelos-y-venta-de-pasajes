-- V4: Tablas para gestión de vuelos e instancias concretas (US-05, US-06, US-08)
CREATE TABLE IF NOT EXISTS vuelos_programados (
                                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                                  codigo_vuelo VARCHAR(20) NOT NULL UNIQUE,
    origen_aeropuerto_id BIGINT NOT NULL,
    destino_aeropuerto_id BIGINT NOT NULL,
    aeronave_id BIGINT NOT NULL,
    hora_salida TIME NOT NULL,
    hora_llegada TIME NOT NULL,
    fecha_inicio_venta DATE NOT NULL,
    fecha_fin_venta DATE NOT NULL,
    dias_semana VARCHAR(50) NOT NULL,
    precio_economy DECIMAL(10,2) NOT NULL,
    precio_primera DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_vuelo_prog_origen FOREIGN KEY (origen_aeropuerto_id) REFERENCES aeropuertos(id),
    CONSTRAINT fk_vuelo_prog_destino FOREIGN KEY (destino_aeropuerto_id) REFERENCES aeropuertos(id),
    CONSTRAINT fk_vuelo_prog_aeronave FOREIGN KEY (aeronave_id) REFERENCES aeronaves(id)
    );

CREATE TABLE IF NOT EXISTS instancias_vuelo (
                                                id BIGINT NOT NULL AUTO_INCREMENT,
                                                vuelo_programado_id BIGINT NOT NULL,
                                                fecha_salida DATE NOT NULL,
                                                fecha_llegada DATE NOT NULL,
                                                hora_salida TIME NOT NULL,
                                                hora_llegada TIME NOT NULL,
                                                asientos_disp_economy INT NOT NULL,
                                                asientos_disp_primera INT NOT NULL,
                                                precio_economy DECIMAL(10,2) NOT NULL,
    precio_primera DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_busqueda_vuelos (fecha_salida, vuelo_programado_id),
    CONSTRAINT fk_instancia_vuelo_prog FOREIGN KEY (vuelo_programado_id) REFERENCES vuelos_programados(id) ON DELETE CASCADE
    );