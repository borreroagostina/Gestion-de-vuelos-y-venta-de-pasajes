package com.aerolinea.gestionvuelos.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Objeto de transferencia para el formulario de alta y edicion de vuelos. */
public class VueloForm {

  private Long id;

  @NotBlank(message = "El código de vuelo es obligatorio")
  private String codigoVuelo;

  @NotNull(message = "Debe seleccionar un aeropuerto de origen")
  private Long origenId;

  @NotNull(message = "Debe seleccionar un aeropuerto de destino")
  private Long destinoId;

  @NotNull(message = "Debe seleccionar una aeronave")
  private Long aeronaveId;

  @NotNull(message = "El horario de salida es obligatorio")
  @org.springframework.format.annotation.DateTimeFormat(pattern = "HH:mm")
  private LocalTime horaSalida;

  @NotNull(message = "El horario de llegada es obligatorio")
  @org.springframework.format.annotation.DateTimeFormat(pattern = "HH:mm")
  private LocalTime horaLlegada;

  @NotNull(message = "La fecha de inicio de venta es obligatoria")
  @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate fechaInicioVenta;

  @NotNull(message = "La fecha de fin de venta es obligatoria")
  @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate fechaFinVenta;

  @NotEmpty(message = "Debe seleccionar al menos un día de operación")
  private List<String> diasSeleccionados = new ArrayList<>();

  @NotNull(message = "El precio de Economy es obligatorio")
  @DecimalMin(value = "0.01", message = "El precio Economy debe ser mayor a cero")
  private BigDecimal precioEconomy;

  @NotNull(message = "El precio de Primera Clase es obligatorio")
  @DecimalMin(value = "0.01", message = "El precio Primera Clase debe ser mayor a cero")
  private BigDecimal precioPrimera;

  public VueloForm() {}

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getCodigoVuelo() {
    return codigoVuelo;
  }

  public void setCodigoVuelo(final String codigoVuelo) {
    this.codigoVuelo = codigoVuelo;
  }

  public Long getOrigenId() {
    return origenId;
  }

  public void setOrigenId(final Long origenId) {
    this.origenId = origenId;
  }

  public Long getDestinoId() {
    return destinoId;
  }

  public void setDestinoId(final Long destinoId) {
    this.destinoId = destinoId;
  }

  public Long getAeronaveId() {
    return aeronaveId;
  }

  public void setAeronaveId(final Long aeronaveId) {
    this.aeronaveId = aeronaveId;
  }

  public LocalTime getHoraSalida() {
    return horaSalida;
  }

  public void setHoraSalida(final LocalTime horaSalida) {
    this.horaSalida = horaSalida;
  }

  public LocalTime getHoraLlegada() {
    return horaLlegada;
  }

  public void setHoraLlegada(final LocalTime horaLlegada) {
    this.horaLlegada = horaLlegada;
  }

  public LocalDate getFechaInicioVenta() {
    return fechaInicioVenta;
  }

  public void setFechaInicioVenta(final LocalDate fechaInicioVenta) {
    this.fechaInicioVenta = fechaInicioVenta;
  }

  public LocalDate getFechaFinVenta() {
    return fechaFinVenta;
  }

  public void setFechaFinVenta(final LocalDate fechaFinVenta) {
    this.fechaFinVenta = fechaFinVenta;
  }

  public List<String> getDiasSeleccionados() {
    return diasSeleccionados;
  }

  public void setDiasSeleccionados(final List<String> diasSeleccionados) {
    this.diasSeleccionados = diasSeleccionados;
  }

  public BigDecimal getPrecioEconomy() {
    return precioEconomy;
  }

  public void setPrecioEconomy(final BigDecimal precioEconomy) {
    this.precioEconomy = precioEconomy;
  }

  public BigDecimal getPrecioPrimera() {
    return precioPrimera;
  }

  public void setPrecioPrimera(final BigDecimal precioPrimera) {
    this.precioPrimera = precioPrimera;
  }
}
