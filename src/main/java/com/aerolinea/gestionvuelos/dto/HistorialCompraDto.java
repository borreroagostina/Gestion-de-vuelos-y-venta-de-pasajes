package com.aerolinea.gestionvuelos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record HistorialCompraDto(
    String orderCode,
    String codigoVuelo,
    String origen,
    String destino,
    LocalDateTime fechaSalida,
    List<PasajeItemDto> pasajes,
    BigDecimal montoTotal,
    LocalDateTime fechaPago) {
  public record PasajeItemDto(
      Long reservaAsientoId,
      String numeroAsiento,
      String nombrePasajero,
      String documentoPasajero) {}
}
