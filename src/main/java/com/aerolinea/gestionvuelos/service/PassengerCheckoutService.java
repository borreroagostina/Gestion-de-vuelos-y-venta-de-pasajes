package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.dto.DatosPasajeroForm;
import com.aerolinea.gestionvuelos.dto.DatosPasajerosForm;
import com.aerolinea.gestionvuelos.dto.ResumenCompraView;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.repository.ReservaTemporalRepository;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassengerCheckoutService {

  private static final BigDecimal IVA_RATE = new BigDecimal("0.21");
  private static final int MONEY_SCALE = 2;

  private final ReservaTemporalRepository reservaTemporalRepository;
  private final ReservaAsientoRepository reservaAsientoRepository;
  private final UsuarioRepository usuarioRepository;

  public PassengerCheckoutService(
      ReservaTemporalRepository reservaTemporalRepository,
      ReservaAsientoRepository reservaAsientoRepository,
      UsuarioRepository usuarioRepository) {
    this.reservaTemporalRepository = reservaTemporalRepository;
    this.reservaAsientoRepository = reservaAsientoRepository;
    this.usuarioRepository = usuarioRepository;
  }

  @Transactional(readOnly = true)
  public boolean tieneReservaActiva(String email, String sesionId) {
    final ReservaTemporal reserva = buscarReservaActiva(email, sesionId);
    if (reserva == null) {
      return false;
    }
    final LocalDateTime ahora = LocalDateTime.now();
    return listarReservaAsientoOrdenada(reserva).stream()
        .anyMatch(
            reservaAsiento ->
                reservaAsiento.getEstado() == EstadoReservaAsiento.RESERVADO_TEMPORAL
                    && reservaAsiento.getFechaExpiracion().isAfter(ahora)
                    && "RESERVADO".equals(reservaAsiento.getAsiento().getEstado()));
  }

  @Transactional(readOnly = true)
  public DatosPasajerosForm crearFormulario(String email, String sesionId) {
    final ReservaTemporal reserva = obtenerReservaActiva(email, sesionId);
    final DatosPasajerosForm form = new DatosPasajerosForm();
    form.setPasajeros(
        listarReservaAsientoOrdenada(reserva).stream()
            .map(this::mapearDatosPasajero)
            .collect(Collectors.toList()));
    return form;
  }

  @Transactional
  public void guardarPasajeros(String email, String sesionId, DatosPasajerosForm form) {
    final ReservaTemporal reserva = obtenerReservaActiva(email, sesionId);
    final List<ReservaAsiento> reservasAsiento = listarReservaAsientoOrdenada(reserva);
    validarCantidadPasajeros(form, reservasAsiento);

    final Map<Long, ReservaAsiento> reservasPorId =
        reservasAsiento.stream()
            .collect(Collectors.toMap(ReservaAsiento::getId, Function.identity()));

    reservasAsiento.forEach(
        reservaAsiento -> {
          final DatosPasajeroForm pasajeroForm =
              form.getPasajeros().stream()
                  .filter(item -> reservaAsiento.getId().equals(item.getReservaAsientoId()))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              "La informacion de los pasajeros no coincide con la reserva activa"));
          final ReservaAsiento actual = reservasPorId.get(pasajeroForm.getReservaAsientoId());
          actualizarDatosPasajero(actual, pasajeroForm);
        });

    reservaAsientoRepository.saveAll(reservasAsiento);
  }

  @Transactional(readOnly = true)
  public ResumenCompraView obtenerResumen(String email, String sesionId) {
    final ReservaTemporal reserva = obtenerReservaActiva(email, sesionId);
    final List<ReservaAsiento> reservasAsiento = listarReservaAsientoOrdenada(reserva);
    if (reservasAsiento.isEmpty()) {
      throw new IllegalStateException("La reserva activa no tiene asientos asociados");
    }

    for (ReservaAsiento item : reservasAsiento) {
      if (!item.tieneDatosPasajeroCompletos()) {
        throw new IllegalStateException(
            "Debe completar los datos de todos los pasajeros antes de ver el resumen");
      }
    }

    final InstanciaVuelo instancia = reservasAsiento.get(0).getAsiento().getInstanciaVuelo();
    final String clase = reservasAsiento.get(0).getAsiento().getClase();
    final BigDecimal precioUnitario = obtenerPrecioUnitario(instancia, clase);
    final BigDecimal subtotal =
        precioUnitario
            .multiply(BigDecimal.valueOf(reservasAsiento.size()))
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    final BigDecimal impuestos =
        subtotal.multiply(IVA_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    final BigDecimal total = subtotal.add(impuestos).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    final List<String> asientos =
        reservasAsiento.stream().map(item -> item.getAsiento().getNumero()).toList();
    final List<ResumenCompraView.PasajeroResumenItem> pasajeros =
        reservasAsiento.stream()
            .map(
                item ->
                    new ResumenCompraView.PasajeroResumenItem(
                        item.getAsiento().getNumero(),
                        item.getNombrePasajero() + " " + item.getApellidoPasajero(),
                        item.getDocumentoPasajero(),
                        item.getFechaNacimientoPasajero()))
            .toList();

    return new ResumenCompraView(
        reserva.getId(),
        instancia.getVueloProgramado().getCodigoVuelo(),
        describirAeropuerto(instancia.getVueloProgramado().getOrigen()),
        describirAeropuerto(instancia.getVueloProgramado().getDestino()),
        instancia.getFechaSalida(),
        instancia.getItinerarioFormateado(),
        traducirClase(clase),
        asientos,
        pasajeros,
        precioUnitario,
        subtotal,
        impuestos,
        total);
  }

  @Transactional(readOnly = true)
  public Long obtenerReservaActivaId(String email, String sesionId) {
    return obtenerReservaActiva(email, sesionId).getId();
  }

  @Transactional(readOnly = true)
  public String obtenerClaseReserva(String email, String sesionId) {
    final ReservaTemporal reserva = obtenerReservaActiva(email, sesionId);
    return listarReservaAsientoOrdenada(reserva).stream()
        .findFirst()
        .map(item -> traducirClase(item.getAsiento().getClase()))
        .orElse("");
  }

  @Transactional(readOnly = true)
  public List<String> obtenerAsientos(String email, String sesionId) {
    final ReservaTemporal reserva = obtenerReservaActiva(email, sesionId);
    return listarReservaAsientoOrdenada(reserva).stream()
        .map(item -> item.getAsiento().getNumero())
        .toList();
  }

  private ReservaTemporal obtenerReservaActiva(String email, String sesionId) {
    final ReservaTemporal reserva = buscarReservaActiva(email, sesionId);
    if (reserva == null) {
      throw new IllegalStateException("No tenes una reserva activa para completar");
    }
    return reserva;
  }

  private ReservaTemporal buscarReservaActiva(String email, String sesionId) {
    final Usuario usuario =
        usuarioRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    final LocalDateTime ahora = LocalDateTime.now();
    if (sesionId != null && !sesionId.isBlank()) {
      return reservaTemporalRepository
          .findFirstByUsuarioIdAndSesionIdAndFechaExpiracionAfterOrderByIdDesc(
              usuario.getId(), sesionId, ahora)
          .orElseGet(
              () ->
                  reservaTemporalRepository
                      .findFirstByUsuarioIdAndFechaExpiracionAfterOrderByIdDesc(
                          usuario.getId(), ahora)
                      .orElse(null));
    }
    return reservaTemporalRepository
        .findFirstByUsuarioIdAndFechaExpiracionAfterOrderByIdDesc(usuario.getId(), ahora)
        .orElse(null);
  }

  private List<ReservaAsiento> listarReservaAsientoOrdenada(ReservaTemporal reserva) {
    return reservaAsientoRepository.findByReservaTemporalIdOrderByAsientoNumeroAsc(reserva.getId());
  }

  private DatosPasajeroForm mapearDatosPasajero(ReservaAsiento reservaAsiento) {
    final DatosPasajeroForm form = new DatosPasajeroForm();
    form.setReservaAsientoId(reservaAsiento.getId());
    form.setNumeroAsiento(reservaAsiento.getAsiento().getNumero());
    form.setClase(traducirClase(reservaAsiento.getAsiento().getClase()));
    form.setNombre(reservaAsiento.getNombrePasajero());
    form.setApellido(reservaAsiento.getApellidoPasajero());
    form.setDocumento(reservaAsiento.getDocumentoPasajero());
    form.setFechaNacimiento(reservaAsiento.getFechaNacimientoPasajero());
    return form;
  }

  private void actualizarDatosPasajero(ReservaAsiento reservaAsiento, DatosPasajeroForm form) {
    reservaAsiento.setNombrePasajero(normalizarTexto(form.getNombre()));
    reservaAsiento.setApellidoPasajero(normalizarTexto(form.getApellido()));
    reservaAsiento.setDocumentoPasajero(normalizarDocumento(form.getDocumento()));
    reservaAsiento.setFechaNacimientoPasajero(form.getFechaNacimiento());
  }

  private void validarCantidadPasajeros(
      DatosPasajerosForm form, List<ReservaAsiento> reservasAsiento) {
    if (form.getPasajeros() == null || form.getPasajeros().size() != reservasAsiento.size()) {
      throw new IllegalArgumentException(
          "La informacion de los pasajeros no coincide con la cantidad de asientos reservados");
    }

    final List<Long> idsEsperados =
        reservasAsiento.stream()
            .map(ReservaAsiento::getId)
            .sorted(Comparator.naturalOrder())
            .toList();
    final List<Long> idsRecibidos =
        form.getPasajeros().stream()
            .map(DatosPasajeroForm::getReservaAsientoId)
            .sorted(Comparator.nullsFirst(Comparator.naturalOrder()))
            .toList();
    if (!idsEsperados.equals(idsRecibidos)) {
      throw new IllegalArgumentException(
          "La informacion de los pasajeros no coincide con la reserva activa");
    }
  }

  private BigDecimal obtenerPrecioUnitario(InstanciaVuelo instancia, String clase) {
    return "PRIMERA".equals(clase) ? instancia.getPrecioPrimera() : instancia.getPrecioEconomy();
  }

  private String describirAeropuerto(com.aerolinea.gestionvuelos.model.Aeropuerto aeropuerto) {
    return aeropuerto.getCodigoIata()
        + " - "
        + aeropuerto.getCiudad()
        + ", "
        + aeropuerto.getNombre();
  }

  private String traducirClase(String clase) {
    return "PRIMERA".equalsIgnoreCase(clase) ? "Primera Clase" : "Economy";
  }

  private String normalizarTexto(String valor) {
    return valor == null ? null : valor.trim();
  }

  private String normalizarDocumento(String valor) {
    return valor == null ? null : valor.trim().toUpperCase(Locale.ROOT);
  }
}
