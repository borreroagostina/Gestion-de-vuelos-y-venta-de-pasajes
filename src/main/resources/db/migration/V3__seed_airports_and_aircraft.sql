INSERT INTO aeropuertos (codigo_iata, nombre, ciudad, pais)
VALUES
    ('EZE', 'Aeropuerto Internacional Ministro Pistarini', 'Buenos Aires', 'Argentina'),
    ('AEP', 'Aeropuerto Internacional Jorge Newbery', 'Buenos Aires', 'Argentina'),
    ('MIA', 'Miami International Airport', 'Miami', 'Estados Unidos'),
    ('GRU', 'Aeroporto Internacional de São Paulo', 'São Paulo', 'Brasil'),
    ('MAD', 'Aeropuerto Adolfo Suárez Madrid-Barajas', 'Madrid', 'España')
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    ciudad = VALUES(ciudad),
    pais = VALUES(pais);

INSERT INTO aeronaves (modelo, fabricante, codigo, economy_seats, primera_clase_seats)
VALUES
    ('Airbus A320', 'Airbus', 'A320-01', 180, 20),
    ('Boeing 737', 'Boeing', 'B737-01', 160, 18)
ON DUPLICATE KEY UPDATE
    fabricante = VALUES(fabricante),
    codigo = VALUES(codigo),
    economy_seats = VALUES(economy_seats),
    primera_clase_seats = VALUES(primera_clase_seats);
