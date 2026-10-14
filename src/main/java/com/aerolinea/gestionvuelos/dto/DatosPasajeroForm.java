package com.aerolinea.gestionvuelos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class DatosPasajeroForm {

  private static final int MAX_NAME_LENGTH = 100;
  private static final int MIN_DOCUMENT_LENGTH = 6;
  private static final int MAX_DOCUMENT_LENGTH = 20;

  @NotNull(message = "La referencia del asiento es obligatoria")
  private Long reservaAsientoId;

  private String numeroAsiento;

  private String clase;

  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = MAX_NAME_LENGTH, message = "El nombre no puede superar 100 caracteres")
  private String nombre;

  @NotBlank(message = "El apellido es obligatorio")
  @Size(max = MAX_NAME_LENGTH, message = "El apellido no puede superar 100 caracteres")
  private String apellido;

  @NotBlank(message = "El DNI/Pasaporte es obligatorio")
  @Size(
      min = MIN_DOCUMENT_LENGTH,
      max = MAX_DOCUMENT_LENGTH,
      message = "El DNI/Pasaporte debe tener entre 6 y 20 caracteres")
  @Pattern(
      regexp = "^[A-Za-z0-9-]+$",
      message = "El DNI/Pasaporte solo puede contener letras, números y guiones")
  private String documento;

  @NotNull(message = "La fecha de nacimiento es obligatoria")
  @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
  private LocalDate fechaNacimiento;

  public Long getReservaAsientoId() {
    return reservaAsientoId;
  }

  public void setReservaAsientoId(Long reservaAsientoId) {
    this.reservaAsientoId = reservaAsientoId;
  }

  public String getNumeroAsiento() {
    return numeroAsiento;
  }

  public void setNumeroAsiento(String numeroAsiento) {
    this.numeroAsiento = numeroAsiento;
  }

  public String getClase() {
    return clase;
  }

  public void setClase(String clase) {
    this.clase = clase;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getApellido() {
    return apellido;
  }

  public void setApellido(String apellido) {
    this.apellido = apellido;
  }

  public String getDocumento() {
    return documento;
  }

  public void setDocumento(String documento) {
    this.documento = documento;
  }

  public LocalDate getFechaNacimiento() {
    return fechaNacimiento;
  }

  public void setFechaNacimiento(LocalDate fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }
}
