package com.aerolinea.gestionvuelos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "aeronaves")
public class Aeronave {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "El modelo es obligatorio")
  @Size(max = 80, message = "El modelo no puede superar 80 caracteres")
  @Column(nullable = false)
  private String modelo;

  @NotBlank(message = "El fabricante es obligatorio")
  @Size(max = 80, message = "El fabricante no puede superar 80 caracteres")
  @Column(nullable = false)
  private String fabricante;

  @NotBlank(message = "El código del avión es obligatorio")
  @Size(max = 30, message = "El código no puede superar 30 caracteres")
  @Column(nullable = false, unique = true)
  private String codigo;

  @NotNull(message = "La cantidad de asientos de Economy es obligatoria")
  @Min(value = 0, message = "Los asientos de Economy no pueden ser negativos")
  @Column(nullable = false)
  private Integer economySeats;

  @NotNull(message = "La cantidad de asientos de Primera Clase es obligatoria")
  @Min(value = 0, message = "Los asientos de Primera Clase no pueden ser negativos")
  @Column(nullable = false)
  private Integer primeraClaseSeats;

  public Aeronave() {}

  public Aeronave(String modelo, String fabricante, String codigo, Integer economySeats, Integer primeraClaseSeats) {
    this.modelo = modelo;
    this.fabricante = fabricante;
    this.codigo = codigo;
    this.economySeats = economySeats;
    this.primeraClaseSeats = primeraClaseSeats;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getModelo() {
    return modelo;
  }

  public void setModelo(String modelo) {
    this.modelo = modelo;
  }

  public String getFabricante() {
    return fabricante;
  }

  public void setFabricante(String fabricante) {
    this.fabricante = fabricante;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public Integer getEconomySeats() {
    return economySeats;
  }

  public void setEconomySeats(Integer economySeats) {
    this.economySeats = economySeats;
  }

  public Integer getPrimeraClaseSeats() {
    return primeraClaseSeats;
  }

  public void setPrimeraClaseSeats(Integer primeraClaseSeats) {
    this.primeraClaseSeats = primeraClaseSeats;
  }
}
