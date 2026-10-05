package com.aerolinea.gestionvuelos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

  private static final int MAX_NAME_LENGTH = 100;
  private static final int MAX_DOCUMENT_LENGTH = 30;
  private static final int MAX_EMAIL_LENGTH = 150;
  private static final int MAX_PHONE_LENGTH = 30;
  private static final int MAX_ROLE_LENGTH = 20;
  private static final int MAX_ACTIVATION_TOKEN_LENGTH = 120;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = MAX_NAME_LENGTH)
  private String nombre;

  @Column(nullable = false, length = MAX_NAME_LENGTH)
  private String apellido;

  @Column(nullable = false, unique = true, length = MAX_DOCUMENT_LENGTH)
  private String documento;

  @Column(nullable = false, unique = true, length = MAX_EMAIL_LENGTH)
  private String email;

  @Column(nullable = false, length = MAX_PHONE_LENGTH)
  private String telefono;

  @Column(nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = MAX_ROLE_LENGTH)
  private RolUsuario rol;

  @Column(nullable = false)
  private boolean activo;

  @Column(length = MAX_ACTIVATION_TOKEN_LENGTH, unique = true)
  private String tokenActivacion;

  @Column private LocalDateTime tokenExpiraEn;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public RolUsuario getRol() {
    return rol;
  }

  public void setRol(RolUsuario rol) {
    this.rol = rol;
  }

  public boolean getActivo() {
    return activo;
  }

  public void setActivo(boolean activo) {
    this.activo = activo;
  }

  public String getTokenActivacion() {
    return tokenActivacion;
  }

  public void setTokenActivacion(String tokenActivacion) {
    this.tokenActivacion = tokenActivacion;
  }

  public LocalDateTime getTokenExpiraEn() {
    return tokenExpiraEn;
  }

  public void setTokenExpiraEn(LocalDateTime tokenExpiraEn) {
    this.tokenExpiraEn = tokenExpiraEn;
  }
}
