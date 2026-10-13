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
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Instancia puntual generada para cada fecha de venta (US-05, US-08). */
@Entity
@Table(name = "instancias_vuelo")
public class InstanciaVuelo {

  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "vuelo_programado_id", nullable = false)
  private VueloProgramado vueloProgramado;

  @Column(nullable = false)
  private LocalDate fechaSalida;

  @Column(nullable = false)
  private LocalDate fechaLlegada;

  @Column(nullable = false)
  private LocalTime horaSalida;

  @Column(nullable = false)
  private LocalTime horaLlegada;

  @Column(nullable = false)
  private Integer asientosDispEconomy;

  @Column(nullable = false)
  private Integer asientosDispPrimera;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal precioEconomy;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal precioPrimera;

  @Column(nullable = false)
  private boolean cancelada = false;

  @Column(name = "motivo_cancelacion")
  private String motivoCancelacion;

  public InstanciaVuelo() {}

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public VueloProgramado getVueloProgramado() {
    return vueloProgramado;
  }

  public void setVueloProgramado(final VueloProgramado vueloProgramado) {
    this.vueloProgramado = vueloProgramado;
  }

  public LocalDate getFechaSalida() {
    return fechaSalida;
  }

  public void setFechaSalida(final LocalDate fechaSalida) {
    this.fechaSalida = fechaSalida;
  }

  public LocalDate getFechaLlegada() {
    return fechaLlegada;
  }

  public void setFechaLlegada(final LocalDate fechaLlegada) {
    this.fechaLlegada = fechaLlegada;
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

  public Integer getAsientosDispEconomy() {
    return asientosDispEconomy;
  }

  public void setAsientosDispEconomy(final Integer asientosDispEconomy) {
    this.asientosDispEconomy = asientosDispEconomy;
  }

  public Integer getAsientosDispPrimera() {
    return asientosDispPrimera;
  }

  public void setAsientosDispPrimera(final Integer asientosDispPrimera) {
    this.asientosDispPrimera = asientosDispPrimera;
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

  public boolean isCancelada() {
    return cancelada;
  }

  public void setCancelada(final boolean cancelada) {
    this.cancelada = cancelada;
  }

  public String getMotivoCancelacion() {
    return motivoCancelacion;
  }

  public void setMotivoCancelacion(final String motivoCancelacion) {
    this.motivoCancelacion = motivoCancelacion;
  }

  public String getDuracionFormateada() {
    final LocalDateTime salida = LocalDateTime.of(fechaSalida, horaSalida);
    final LocalDateTime llegada = LocalDateTime.of(fechaLlegada, horaLlegada);
    final Duration duracion = Duration.between(salida, llegada);
    final long horas = duracion.toHours();
    final long minutos = duracion.toMinutesPart();
    return horas + "h " + minutos + "m";
  }

  public String getItinerarioFormateado() {
    final String salida =
        horaSalida.format(TIME_FORMATTER).toLowerCase(Locale.ROOT)
            + " ("
            + fechaSalida.format(DATE_FORMATTER)
            + ")";
    final String llegada = horaLlegada.format(TIME_FORMATTER).toLowerCase(Locale.ROOT);

    if (cruzaMedianoche()) {
      return salida
          + " → "
          + llegada
          + " del día siguiente ("
          + fechaLlegada.format(DATE_FORMATTER)
          + ")";
    }
    return salida + " → " + llegada + " (" + fechaLlegada.format(DATE_FORMATTER) + ")";
  }

  public boolean cruzaMedianoche() {
    return fechaLlegada != null && fechaSalida != null && fechaLlegada.isAfter(fechaSalida);
  }
}
