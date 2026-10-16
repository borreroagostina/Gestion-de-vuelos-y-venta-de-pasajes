package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.AsientoRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.repository.ReservaTemporalRepository;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

  private static final int MAX_ASIENTOS_POR_RESERVA = 9;

  private final AsientoRepository asientoRepo;
  private final ReservaAsientoRepository reservaAsientoRepo;
  private final ReservaTemporalRepository reservaRepo;
  private final UsuarioRepository usuarioRepo;

  @Value("${app.reserva.expiracion-minutos:10}")
  private int expiracionMinutos;

  public BookingService(
      AsientoRepository asientoRepo,
      ReservaAsientoRepository reservaAsientoRepo,
      ReservaTemporalRepository reservaRepo,
      UsuarioRepository usuarioRepo) {
    this.asientoRepo = asientoRepo;
    this.reservaAsientoRepo = reservaAsientoRepo;
    this.reservaRepo = reservaRepo;
    this.usuarioRepo = usuarioRepo;
  }

  @Transactional
  public List<Asiento> listarAsientosDeInstancia(Long instanciaId) {
    liberarReservasExpiradasEn(LocalDateTime.now());
    return asientoRepo.findByInstanciaVueloIdOrderByNumeroAsc(instanciaId);
  }

  @Transactional
  public ReservaTemporal reservarAsientos(Long usuarioId, String sesionId, List<Long> asientoIds) {
    return reservarAsientos(usuarioId, sesionId, asientoIds, null);
  }

  @Transactional
  public ReservaTemporal reservarAsientos(
      Long usuarioId, String sesionId, List<Long> asientoIds, String claseSeleccionada) {
    liberarReservasExpiradasEn(LocalDateTime.now());
    if (asientoIds == null || asientoIds.isEmpty()) {
      throw new IllegalArgumentException("Debe seleccionar al menos un asiento");
    }
    if (asientoIds.size() > MAX_ASIENTOS_POR_RESERVA) {
      throw new IllegalArgumentException("No se pueden reservar mas de 9 asientos");
    }

    // Lock pesimista para evitar concurrencia en la seleccion del mismo asiento
    final List<Asiento> asientos = asientoRepo.findAllByIdWithLock(asientoIds);
    if (asientos.size() != asientoIds.size()) {
      throw new IllegalArgumentException("Algunos asientos seleccionados no existen");
    }

    final String claseOperacion = asientos.get(0).getClase();
    for (Asiento a : asientos) {
      if (!claseOperacion.equals(a.getClase())) {
        throw new IllegalArgumentException(
            "Debe seleccionar asientos de una sola clase por operación");
      }
      if (!"DISPONIBLE".equals(a.getEstado())) {
        throw new IllegalStateException(
            "El asiento " + a.getNumero() + " acaba de ser tomado por otro usuario. Elegi otro.");
      }
    }
    final String claseSeleccionadaNormalizada = normalizarClase(claseSeleccionada);
    if (esClaseValida(claseSeleccionadaNormalizada)
        && !claseOperacion.equalsIgnoreCase(claseSeleccionadaNormalizada)) {
      throw new IllegalArgumentException(
          "La clase seleccionada no coincide con los asientos elegidos");
    }

    final Usuario usuario =
        usuarioRepo
            .findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

    final ReservaTemporal reserva = new ReservaTemporal();
    reserva.setUsuario(usuario);
    reserva.setSesionId(sesionId);
    reserva.setFechaExpiracion(LocalDateTime.now().plusMinutes(expiracionMinutos));
    final ReservaTemporal reservaGuardada = reservaRepo.save(reserva);

    final LocalDateTime fechaExpiracion = reservaGuardada.getFechaExpiracion();
    final List<ReservaAsiento> reservasAsiento = new java.util.ArrayList<>();
    for (Asiento a : asientos) {
      a.setEstado("RESERVADO");
      reservaGuardada.getAsientos().add(a);
      final ReservaAsiento reservaAsiento = new ReservaAsiento();
      reservaAsiento.setReservaTemporal(reservaGuardada);
      reservaAsiento.setUsuario(usuario);
      reservaAsiento.setSesionId(sesionId);
      reservaAsiento.setAsiento(a);
      reservaAsiento.setEstado(EstadoReservaAsiento.RESERVADO_TEMPORAL);
      reservaAsiento.setFechaExpiracion(fechaExpiracion);
      reservasAsiento.add(reservaAsiento);
    }

    reservaAsientoRepo.saveAll(reservasAsiento);
    asientoRepo.saveAll(asientos);
    return reservaRepo.save(reservaGuardada);
  }

  @Scheduled(fixedRateString = "${app.reserva.job-rate-ms:60000}")
  @Transactional
  public void liberarReservasExpiradas() {
    final LocalDateTime ahora = LocalDateTime.now();
    liberarReservasExpiradasEn(ahora);
  }

  @Transactional
  public long contarDisponibles(Long instanciaId, String clase) {
    liberarReservasExpiradasEn(LocalDateTime.now());
    final List<Asiento> asientosClase =
        asientoRepo.findByInstanciaVueloIdOrderByNumeroAsc(instanciaId).stream()
            .filter(asiento -> clase.equals(asiento.getClase()))
            .toList();
    if (asientosClase.isEmpty()) {
      return 0;
    }

    final InstanciaVuelo instancia = asientosClase.get(0).getInstanciaVuelo();
    final int capacidad =
        "PRIMERA".equals(clase)
            ? instancia.getVueloProgramado().getAeronave().getPrimeraClaseSeats()
            : instancia.getVueloProgramado().getAeronave().getEconomySeats();

    final long reservadosTemporales =
        reservaAsientoRepo.countByInstanciaAndClaseAndEstadoActivo(
            instanciaId, clase, EstadoReservaAsiento.RESERVADO_TEMPORAL, LocalDateTime.now());
    final long vendidos =
        asientoRepo.countByInstanciaVueloIdAndClaseAndEstadoIn(
            instanciaId, clase, java.util.List.of("OCUPADO", "VENDIDO"));
    final long disponibles = (long) capacidad - reservadosTemporales - vendidos;
    return Math.max(disponibles, 0L);
  }

  private void liberarReserva(ReservaTemporal reserva) {
    final List<ReservaAsiento> detalle =
        reservaAsientoRepo.findByReservaTemporalId(reserva.getId());
    for (ReservaAsiento item : detalle) {
      if (item.getEstado() == EstadoReservaAsiento.RESERVADO_TEMPORAL) {
        item.setEstado(EstadoReservaAsiento.LIBERADO);
      }
    }
    reservaAsientoRepo.saveAll(detalle);

    for (Asiento a : reserva.getAsientos()) {
      if ("RESERVADO".equals(a.getEstado())) {
        a.setEstado("DISPONIBLE");
        asientoRepo.save(a);
      }
    }
  }

  private void liberarReservasExpiradasEn(LocalDateTime ahora) {
    final List<ReservaTemporal> expiradas = reservaRepo.findExpiredWithLock(ahora);
    for (ReservaTemporal r : expiradas) {
      liberarReserva(r);
    }
  }

  private String normalizarClase(String claseSeleccionada) {
    if (claseSeleccionada == null) {
      return "";
    }
    return claseSeleccionada.replace("'", "").replace("\"", "").trim().toUpperCase();
  }

  private boolean esClaseValida(String clase) {
    return "ECONOMY".equals(clase) || "PRIMERA".equals(clase);
  }
}
