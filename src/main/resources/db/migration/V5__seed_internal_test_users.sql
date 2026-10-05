INSERT INTO usuarios (nombre, apellido, documento, email, telefono, password_hash, rol, activo, token_activacion, token_expira_en)
VALUES
    ('Admin', 'Sistema', 'ADMIN-0001', 'admin@aerolinea.local', '+54-11-0000-0001', '$2a$10$Q8fuA/eKm96xl0tuXuO9Eu682F75/oItSJzFjiQQ6Y4nRKPLzwScq', 'ADMIN', TRUE, NULL, NULL),
    ('Empleado', 'Mostrador', 'EMPL-0001', 'empleado@aerolinea.local', '+54-11-0000-0002', '$2a$10$eTPxTFMdYIKHV07qyDmei.jEUy5b5nEtj1wHgyqGXP3cwMIE3vBOy', 'EMPLEADO', TRUE, NULL, NULL)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    apellido = VALUES(apellido),
    telefono = VALUES(telefono),
    password_hash = VALUES(password_hash),
    rol = VALUES(rol),
    activo = VALUES(activo),
    token_activacion = VALUES(token_activacion),
    token_expira_en = VALUES(token_expira_en);
