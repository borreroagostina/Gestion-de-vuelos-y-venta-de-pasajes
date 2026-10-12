package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.AsientoRepository;
import com.aerolinea.gestionvuelos.repository.InstanciaVueloRepository;
import com.aerolinea.gestionvuelos.repository.ReservaTemporalRepository;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final AsientoRepository asientoRepo;
    private final ReservaTemporalRepository reservaRepo;
    private final InstanciaVueloRepository instanciaRepo;
    private final UsuarioRepository usuarioRepo;

    @Value("${app.reserva.expiracion-minutos:10}")
    private int expiracionMinutos;

    public BookingService(
            AsientoRepository asientoRepo,
            ReservaTemporalRepository reservaRepo,
            InstanciaVueloRepository instanciaRepo,
            UsuarioRepository usuarioRepo) {
        this.asientoRepo = asientoRepo;
        this.reservaRepo = reservaRepo;
        this.instanciaRepo = instanciaRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public List<Asiento> listarAsientosDeInstancia(Long instanciaId) {
        return asientoRepo.findByInstanciaVueloIdOrderByNumeroAsc(instanciaId);
    }

    @Transactional
    public ReservaTemporal reservarAsientos(Long usuarioId, List<Long> asientoIds) {
        if (asientoIds == null || asientoIds.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un asiento");
        }
        if (asientoIds.size() > 9) {
            throw new IllegalArgumentException("No se pueden reservar mas de 9 asientos");
        }

        // Lock pesimista para evitar concurrencia en la seleccion del mismo asiento
        List<Asiento> asientos = asientoRepo.findAllByIdWithLock(asientoIds);
        if (asientos.size() != asientoIds.size()) {
            throw new IllegalArgumentException("Algunos asientos seleccionados no existen");
        }

        for (Asiento a : asientos) {
            if (!"DISPONIBLE".equals(a.getEstado())) {
                throw new IllegalStateException("El asiento " + a.getNumero() + " ya no esta disponible");
            }
        }

        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        ReservaTemporal reserva = new ReservaTemporal();
        reserva.setUsuario(usuario);
        reserva.setFechaExpiracion(LocalDateTime.now().plusMinutes(expiracionMinutos));

        for (Asiento a : asientos) {
            a.setEstado("RESERVADO");
            reserva.getAsientos().add(a);
        }

        asientoRepo.saveAll(asientos);
        return reservaRepo.save(reserva);
    }

    @Scheduled(fixedRateString = "${app.reserva.job-rate-ms:60000}")
    @Transactional
    public void liberarReservasExpiradas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<ReservaTemporal> expiradas = reservaRepo.findByFechaExpiracionBefore(ahora);
        for (ReservaTemporal r : expiradas) {
            for (Asiento a : r.getAsientos()) {
                if ("RESERVADO".equals(a.getEstado())) {
                    a.setEstado("DISPONIBLE");
                    asientoRepo.save(a);
                }
            }
            reservaRepo.delete(r);
        }
    }

    public long contarDisponibles(Long instanciaId, String clase) {
        return asientoRepo.countByInstanciaVueloIdAndClaseAndEstado(instanciaId, clase, "DISPONIBLE");
    }
}
