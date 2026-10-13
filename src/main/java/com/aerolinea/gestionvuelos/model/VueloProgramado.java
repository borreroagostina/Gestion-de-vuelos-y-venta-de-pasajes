package com.aerolinea.gestionvuelos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Representa la programacion recurrente de un vuelo (US-05). */
@Entity
@Table(name = "vuelos_programados")
public class VueloProgramado {

  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);

  private static final int CODIGO_VUELO_LENGTH = 20;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "El codigo de vuelo es obligatorio")
  @Column(nullable = false, unique = true, length = CODIGO_VUELO_LENGTH)
  private String codigoVuelo;

  @NotNull(message = "El aeropuerto de origen es obligatorio")
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "origen_aeropuerto_id", nullable = false)
  private Aeropuerto origen;

  @NotNull(message = "El aeropuerto de destino es obligatorio")
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "destino_aeropuerto_id", nullable = false)
  private Aeropuerto destino;

  @NotNull(message = "La aeronave asignada es obligatoria")
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "aeronave_id", nullable = false)
  private Aeronave aeronave;

  @NotNull(message = "El horario de salida es obligatorio")
  @Column(nullable = false)
  private LocalTime horaSalida;

  @NotNull(message = "El horario de llegada es obligatorio")
  @Column(nullable = false)
  private LocalTime horaLlegada;

  @NotNull(message = "La fecha de inicio de venta es obligatoria")
  @Column(nullable = false)
  private LocalDate fechaInicioVenta;

  @NotNull(message = "La fecha de fin de venta es obligatoria")
  @Column(nullable = false)
  private LocalDate fechaFinVenta;

  @NotBlank(message = "Debe seleccionar al menos un dia de operacion")
  @Column(name = "dias_semana", nullable = false, length = 100)
  private String diasSemana;

  @NotNull(message = "El precio de Economy es obligatorio")
  @DecimalMin(value = "0.01", message = "El precio de Economy debe ser mayor a cero")
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal precioEconomy;

  @NotNull(message = "El precio de Primera Clase es obligatorio")
  @DecimalMin(value = "0.01", message = "El precio de Primera Clase debe ser mayor a cero")
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal precioPrimera;

  @Column(nullable = false)
  private boolean cancelado = false;

  @Column(name = "motivo_cancelacion")
  private String motivoCancelacion;

  public VueloProgramado() {}

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

  public Aeropuerto getOrigen() {
    return origen;
  }

  public void setOrigen(final Aeropuerto origen) {
    this.origen = origen;
  }

  public Aeropuerto getDestino() {
    return destino;
  }

  public void setDestino(final Aeropuerto destino) {
    this.destino = destino;
  }

  public Aeronave getAeronave() {
    return aeronave;
  }

  public void setAeronave(final Aeronave aeronave) {
    this.aeronave = aeronave;
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

  public String getDiasSemana() {
    return diasSemana;
  }

  public void setDiasSemana(final String diasSemana) {
    this.diasSemana = diasSemana;
  }

  public String getDiasSemanaEnEspanol() {
    if (diasSemana == null || diasSemana.isBlank()) {
      return "";
    }
    return diasSemana
        .replace("MONDAY", "Lunes")
        .replace("TUESDAY", "Martes")
        .replace("WEDNESDAY", "Miércoles")
        .replace("THURSDAY", "Jueves")
        .replace("FRIDAY", "Viernes")
        .replace("SATURDAY", "Sábado")
        .replace("SUNDAY", "Domingo");
  }

  public String getHorarioFormateadoAdmin() {
    if (horaSalida == null || horaLlegada == null) {
      return "";
    }
    final String salida = horaSalida.format(TIME_FORMATTER).toLowerCase(Locale.ROOT);
    final String llegada = horaLlegada.format(TIME_FORMATTER).toLowerCase(Locale.ROOT);

    if (llegaAlDiaSiguiente()) {
      return salida + " a " + llegada + " del día siguiente";
    }
    return salida + " a " + llegada;
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

  public boolean isCancelado() {
    return cancelado;
  }

  public void setCancelado(final boolean cancelado) {
    this.cancelado = cancelado;
  }

  public String getMotivoCancelacion() {
    return motivoCancelacion;
  }

  public void setMotivoCancelacion(final String motivoCancelacion) {
    this.motivoCancelacion = motivoCancelacion;
  }

  public boolean llegaAlDiaSiguiente() {
    return horaLlegada != null && horaSalida != null && horaLlegada.isBefore(horaSalida);
  }

  public String getPeriodoVentaFormateado() {
    if (fechaInicioVenta == null || fechaFinVenta == null) {
      return "";
    }
    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    return fechaInicioVenta.format(dtf) + " al " + fechaFinVenta.format(dtf);
  }
}
