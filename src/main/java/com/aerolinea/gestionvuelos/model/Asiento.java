package com.aerolinea.gestionvuelos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "asientos")
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_vuelo_id", nullable = false)
    private InstanciaVuelo instanciaVuelo;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(nullable = false, length = 20)
    private String clase; // ECONOMY, PRIMERA

    @Column(nullable = false, length = 20)
    private String estado = "DISPONIBLE"; // DISPONIBLE, RESERVADO, OCUPADO

    public Asiento() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InstanciaVuelo getInstanciaVuelo() {
        return instanciaVuelo;
    }

    public void setInstanciaVuelo(InstanciaVuelo instanciaVuelo) {
        this.instanciaVuelo = instanciaVuelo;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getClase() {
        return clase;
    }

    public void setClase(String clase) {
        this.clase = clase;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
