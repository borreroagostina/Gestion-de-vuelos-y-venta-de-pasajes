package com.aerolinea.gestionvuelos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OcupacionVueloDto(
    String codigoVuelo,
    LocalDate fechaSalida,
    String origen,
    String destino,
    String clase,
    long vendidos,
    int capacidad,
    BigDecimal porcentajeOcupacion) {}
