package com.aerolinea.gestionvuelos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaCheckoutContext(
    Long reservaId,
    String codigoVuelo,
    String origen,
    String destino,
    String fechaHoraVuelo,
    String clase,
    List<String> asientos,
    BigDecimal precioUnitario,
    int cantidadAsientos,
    LocalDateTime fechaExpiracion) {}
