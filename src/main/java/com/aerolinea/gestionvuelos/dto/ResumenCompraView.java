package com.aerolinea.gestionvuelos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ResumenCompraView {

  private final Long reservaId;
  private final String codigoVuelo;
  private final String origen;
  private final String destino;
  private final LocalDate fechaSalida;
  private final String itinerario;
  private final String clase;
  private final List<String> asientos;
  private final List<PasajeroResumenItem> pasajeros;
  private final BigDecimal precioUnitario;
  private final BigDecimal subtotal;
  private final BigDecimal impuestos;
  private final BigDecimal total;

  public ResumenCompraView(
      Long reservaId,
      String codigoVuelo,
      String origen,
      String destino,
      LocalDate fechaSalida,
      String itinerario,
      String clase,
      List<String> asientos,
      List<PasajeroResumenItem> pasajeros,
      BigDecimal precioUnitario,
      BigDecimal subtotal,
      BigDecimal impuestos,
      BigDecimal total) {
    this.reservaId = reservaId;
    this.codigoVuelo = codigoVuelo;
    this.origen = origen;
    this.destino = destino;
    this.fechaSalida = fechaSalida;
    this.itinerario = itinerario;
    this.clase = clase;
    this.asientos = asientos;
    this.pasajeros = pasajeros;
    this.precioUnitario = precioUnitario;
    this.subtotal = subtotal;
    this.impuestos = impuestos;
    this.total = total;
  }

  public Long getReservaId() {
    return reservaId;
  }

  public String getCodigoVuelo() {
    return codigoVuelo;
  }

  public String getOrigen() {
    return origen;
  }

  public String getDestino() {
    return destino;
  }

  public LocalDate getFechaSalida() {
    return fechaSalida;
  }

  public String getItinerario() {
    return itinerario;
  }

  public String getClase() {
    return clase;
  }

  public List<String> getAsientos() {
    return asientos;
  }

  public List<PasajeroResumenItem> getPasajeros() {
    return pasajeros;
  }

  public BigDecimal getPrecioUnitario() {
    return precioUnitario;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public BigDecimal getImpuestos() {
    return impuestos;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public static class PasajeroResumenItem {

    private final String asiento;
    private final String nombreCompleto;
    private final String documento;
    private final LocalDate fechaNacimiento;

    public PasajeroResumenItem(
        String asiento, String nombreCompleto, String documento, LocalDate fechaNacimiento) {
      this.asiento = asiento;
      this.nombreCompleto = nombreCompleto;
      this.documento = documento;
      this.fechaNacimiento = fechaNacimiento;
    }

    public String getAsiento() {
      return asiento;
    }

    public String getNombreCompleto() {
      return nombreCompleto;
    }

    public String getDocumento() {
      return documento;
    }

    public LocalDate getFechaNacimiento() {
      return fechaNacimiento;
    }
  }
}
