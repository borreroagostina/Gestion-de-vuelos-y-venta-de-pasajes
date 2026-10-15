package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.dto.FormValidationIssue;
import com.aerolinea.gestionvuelos.dto.PasajeroResumenItem;
import com.aerolinea.gestionvuelos.dto.PasajeroViajeForm;
import com.aerolinea.gestionvuelos.dto.ReservaCheckoutContext;
import com.aerolinea.gestionvuelos.dto.ResumenCompraDto;
import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.repository.ReservaTemporalRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

  private static final BigDecimal TASA_IMPUESTOS = new BigDecimal("0.21");
  private static final String CLASE_PRIMERA = "PRIMERA";
  private static final String TIPO_DNI = "DNI";
  private static final String TIPO_PASAPORTE = "PASAPORTE";
  private static final String REGEX_DNI = "^\\d{7,8}$";
  private static final String REGEX_PASAPORTE = "^[A-Z0-9]{6,12}$";

  private final ReservaTemporalRepository reservaTemporalRepository;

  public CheckoutService(ReservaTemporalRepository reservaTemporalRepository) {
    this.reservaTemporalRepository = reservaTemporalRepository;
  }

  @Transactional(readOnly = true)
  public Optional<ReservaCheckoutContext> obtenerReservaActiva(Long usuarioId, String sesionId) {
    final LocalDateTime ahora = LocalDateTime.now();
    final Optional<ReservaTemporal> reservaOpt = buscarReservaActiva(usuarioId, sesionId, ahora);
    if (reservaOpt.isEmpty()) {
      return Optional.empty();
    }

    final ReservaTemporal reserva = reservaOpt.get();
    final List<Asiento> asientosOrdenados =
        reserva.getAsientos().stream().sorted(Comparator.comparing(Asiento::getNumero)).toList();

    if (asientosOrdenados.isEmpty()) {
      return Optional.empty();
    }

    final Asiento primerAsiento = asientosOrdenados.get(0);
    final InstanciaVuelo instancia = primerAsiento.getInstanciaVuelo();
    final String clase = primerAsiento.getClase();
    final BigDecimal precioUnitario =
        CLASE_PRIMERA.equals(clase) ? instancia.getPrecioPrimera() : instancia.getPrecioEconomy();

    final List<String> numerosAsiento = asientosOrdenados.stream().map(Asiento::getNumero).toList();

    return Optional.of(
        new ReservaCheckoutContext(
            reserva.getId(),
            instancia.getVueloProgramado().getCodigoVuelo(),
            instancia.getVueloProgramado().getOrigen().getCiudad(),
            instancia.getVueloProgramado().getDestino().getCiudad(),
            instancia.getItinerarioFormateado(),
            clase,
            numerosAsiento,
            precioUnitario,
            asientosOrdenados.size(),
            reserva.getFechaExpiracion()));
  }

  public List<FormValidationIssue> validarPasajeros(
      List<PasajeroViajeForm> pasajeros, int cantidadEsperada) {
    final List<FormValidationIssue> issues = new ArrayList<>();
    if (pasajeros == null || pasajeros.size() != cantidadEsperada) {
      issues.add(
          new FormValidationIssue(
              "pasajeros",
              "Debes completar exactamente " + cantidadEsperada + " pasajeros para continuar."));
      return issues;
    }

    for (int i = 0; i < pasajeros.size(); i++) {
      final PasajeroViajeForm pasajero = pasajeros.get(i);
      if (pasajero == null) {
        issues.add(
            new FormValidationIssue(
                "pasajeros[" + i + "]", "Faltan datos de uno de los pasajeros del viaje."));
        continue;
      }

      final String tipoDocumento = normalizar(pasajero.getTipoDocumento());
      final String numeroDocumento = normalizar(pasajero.getNumeroDocumento());
      issues.addAll(validarDocumento(i, tipoDocumento, numeroDocumento));
    }
    return issues;
  }

  public ResumenCompraDto construirResumen(
      ReservaCheckoutContext reserva, List<PasajeroViajeForm> pasajeros) {
    if (pasajeros == null || pasajeros.size() != reserva.cantidadAsientos()) {
      throw new IllegalArgumentException("La cantidad de pasajeros no coincide con los asientos.");
    }

    final BigDecimal subtotal =
        reserva
            .precioUnitario()
            .multiply(BigDecimal.valueOf(reserva.cantidadAsientos()))
            .setScale(2, RoundingMode.HALF_UP);
    final BigDecimal impuestos =
        subtotal.multiply(TASA_IMPUESTOS).setScale(2, RoundingMode.HALF_UP);
    final BigDecimal total = subtotal.add(impuestos).setScale(2, RoundingMode.HALF_UP);

    final List<PasajeroResumenItem> items = new ArrayList<>();
    for (int i = 0; i < reserva.asientos().size(); i++) {
      final PasajeroViajeForm pasajero = pasajeros.get(i);
      items.add(
          new PasajeroResumenItem(
              reserva.asientos().get(i),
              pasajero.getNombre() + " " + pasajero.getApellido(),
              pasajero.getTipoDocumento(),
              pasajero.getNumeroDocumento()));
    }

    return new ResumenCompraDto(subtotal, impuestos, total, items);
  }

  public long minutosRestantes(LocalDateTime fechaExpiracion) {
    return Math.max(0, Duration.between(LocalDateTime.now(), fechaExpiracion).toMinutes());
  }

  private Optional<ReservaTemporal> buscarReservaActiva(
      Long usuarioId, String sesionId, LocalDateTime ahora) {
    if (sesionId != null && !sesionId.isBlank()) {
      return reservaTemporalRepository
          .findFirstByUsuarioIdAndSesionIdAndFechaExpiracionAfterOrderByFechaExpiracionDesc(
              usuarioId, sesionId, ahora);
    }

    return reservaTemporalRepository
        .findFirstByUsuarioIdAndFechaExpiracionAfterOrderByFechaExpiracionDesc(usuarioId, ahora);
  }

  private List<FormValidationIssue> validarDocumento(
      int indice, String tipoDocumento, String numeroDocumento) {
    final List<FormValidationIssue> issues = new ArrayList<>();
    if (TIPO_DNI.equals(tipoDocumento)) {
      if (!numeroDocumento.matches(REGEX_DNI)) {
        issues.add(
            new FormValidationIssue(
                "pasajeros[" + indice + "].numeroDocumento", "El DNI debe tener 7 u 8 dígitos."));
      }
      return issues;
    }

    if (TIPO_PASAPORTE.equals(tipoDocumento)) {
      if (!numeroDocumento.matches(REGEX_PASAPORTE)) {
        issues.add(
            new FormValidationIssue(
                "pasajeros[" + indice + "].numeroDocumento",
                "El pasaporte debe tener entre 6 y 12 caracteres alfanuméricos."));
      }
      return issues;
    }

    issues.add(
        new FormValidationIssue(
            "pasajeros[" + indice + "].tipoDocumento",
            "Selecciona un tipo de documento válido (DNI o PASAPORTE)."));
    return issues;
  }

  private String normalizar(String valor) {
    if (valor == null) {
      return "";
    }
    return valor.trim().toUpperCase();
  }
}
