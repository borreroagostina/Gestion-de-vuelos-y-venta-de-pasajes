package com.aerolinea.gestionvuelos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenCompraDto(
    BigDecimal subtotal,
    BigDecimal impuestos,
    BigDecimal total,
    List<PasajeroResumenItem> pasajeros) {}
