package com.aerolinea.gestionvuelos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "aeropuertos")
public class Aeropuerto {

  private static final int MAX_NAME_LENGTH = 150;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "El código IATA es obligatorio")
  @Size(min = 3, max = 10, message = "El código IATA debe tener entre 3 y 10 caracteres")
  @Column(nullable = false, unique = true)
  private String codigoIata;

  @NotBlank(message = "El nombre del aeropuerto es obligatorio")
  @Size(max = MAX_NAME_LENGTH, message = "El nombre no puede superar 150 caracteres")
  @Column(nullable = false)
  private String nombre;

  @NotBlank(message = "La ciudad es obligatoria")
  @Size(max = 100, message = "La ciudad no puede superar 100 caracteres")
  @Column(nullable = false)
  private String ciudad;

  @NotBlank(message = "El país es obligatorio")
  @Size(max = 100, message = "El país no puede superar 100 caracteres")
  @Column(nullable = false)
  private String pais;

  public Aeropuerto() {}

  public Aeropuerto(String codigoIata, String nombre, String ciudad, String pais) {
    this.codigoIata = codigoIata;
    this.nombre = nombre;
    this.ciudad = ciudad;
    this.pais = pais;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCodigoIata() {
    return codigoIata;
  }

  public void setCodigoIata(String codigoIata) {
    this.codigoIata = codigoIata;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getCiudad() {
    return ciudad;
  }

  public void setCiudad(String ciudad) {
    this.ciudad = ciudad;
  }

  public String getPais() {
    return pais;
  }

  public void setPais(String pais) {
    this.pais = pais;
  }
}
