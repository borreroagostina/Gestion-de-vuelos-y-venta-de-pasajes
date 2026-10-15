package com.aerolinea.gestionvuelos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class PasajeroViajeForm {

  private static final int MAX_NAME_LENGTH = 100;
  private static final int MAX_DOCUMENT_LENGTH = 30;

  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = MAX_NAME_LENGTH, message = "El nombre no puede superar 100 caracteres")
  private String nombre;

  @NotBlank(message = "El apellido es obligatorio")
  @Size(max = MAX_NAME_LENGTH, message = "El apellido no puede superar 100 caracteres")
  private String apellido;

  @NotBlank(message = "Debe seleccionar tipo de documento")
  private String tipoDocumento;

  @NotBlank(message = "El número de documento es obligatorio")
  @Size(max = MAX_DOCUMENT_LENGTH, message = "El documento no puede superar 30 caracteres")
  private String numeroDocumento;

  @NotNull(message = "La fecha de nacimiento es obligatoria")
  @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
  private LocalDate fechaNacimiento;

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

  public String getTipoDocumento() {
    return tipoDocumento;
  }

  public void setTipoDocumento(String tipoDocumento) {
    this.tipoDocumento = tipoDocumento;
  }

  public String getNumeroDocumento() {
    return numeroDocumento;
  }

  public void setNumeroDocumento(String numeroDocumento) {
    this.numeroDocumento = numeroDocumento;
  }

  public LocalDate getFechaNacimiento() {
    return fechaNacimiento;
  }

  public void setFechaNacimiento(LocalDate fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }
}
