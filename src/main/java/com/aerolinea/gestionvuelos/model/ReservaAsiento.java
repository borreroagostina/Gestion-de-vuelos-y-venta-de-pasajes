package com.aerolinea.gestionvuelos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserva_asiento")
public class ReservaAsiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_temporal_id", nullable = false)
    private ReservaTemporal reservaTemporal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_id", nullable = false)
    private Asiento asiento;

    @Column(name = "sesion_id", length = 100)
    private String sesionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoReservaAsiento estado;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReservaTemporal getReservaTemporal() {
        return reservaTemporal;
    }

    public void setReservaTemporal(ReservaTemporal reservaTemporal) {
        this.reservaTemporal = reservaTemporal;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Asiento getAsiento() {
        return asiento;
    }

    public void setAsiento(Asiento asiento) {
        this.asiento = asiento;
    }

    public String getSesionId() {
        return sesionId;
    }

    public void setSesionId(String sesionId) {
        this.sesionId = sesionId;
    }

    public EstadoReservaAsiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoReservaAsiento estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}
