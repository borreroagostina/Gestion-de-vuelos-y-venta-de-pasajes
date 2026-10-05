package com.aerolinea.gestionvuelos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistroUsuarioForm {

  private static final int MAX_NAME_LENGTH = 100;
  private static final int MIN_DOCUMENT_LENGTH = 6;
  private static final int MAX_DOCUMENT_LENGTH = 30;
  private static final int MAX_EMAIL_LENGTH = 150;
  private static final int MIN_PASSWORD_LENGTH = 8;
  private static final int MAX_PASSWORD_LENGTH = 72;

  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = MAX_NAME_LENGTH, message = "El nombre no puede superar 100 caracteres")
  private String nombre;

  @NotBlank(message = "El apellido es obligatorio")
  @Size(max = MAX_NAME_LENGTH, message = "El apellido no puede superar 100 caracteres")
  private String apellido;

  @NotBlank(message = "El documento es obligatorio")
  @Size(
      min = MIN_DOCUMENT_LENGTH,
      max = MAX_DOCUMENT_LENGTH,
      message = "El documento debe tener entre 6 y 30 caracteres")
  private String documento;

  @NotBlank(message = "El email es obligatorio")
  @Email(message = "El formato del email es inválido")
  @Size(max = MAX_EMAIL_LENGTH, message = "El email no puede superar 150 caracteres")
  private String email;

  @NotBlank(message = "El teléfono es obligatorio")
  @Pattern(
      regexp = "^[0-9+()\\-\\s]{7,30}$",
      message = "El teléfono debe contener solo números y caracteres válidos")
  private String telefono;

  @NotBlank(message = "La contraseña es obligatoria")
  @Size(
      min = MIN_PASSWORD_LENGTH,
      max = MAX_PASSWORD_LENGTH,
      message = "La contraseña debe tener entre 8 y 72 caracteres")
  private String password;

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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
