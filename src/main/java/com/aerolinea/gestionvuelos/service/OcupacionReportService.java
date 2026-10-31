package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.dto.AsientosVendidosPorClase;
import com.aerolinea.gestionvuelos.dto.OcupacionVueloDto;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.VueloProgramado;
import com.aerolinea.gestionvuelos.repository.InstanciaVueloRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.repository.VueloProgramadoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OcupacionReportService {

  public static final String TODAS_LAS_CLASES = "TODAS";
  private static final String ECONOMY = "ECONOMY";
  private static final String PRIMERA = "PRIMERA";
  private static final BigDecimal PORCENTAJE = new BigDecimal("100");

  private final InstanciaVueloRepository instanciaVueloRepository;
  private final ReservaAsientoRepository reservaAsientoRepository;
  private final VueloProgramadoRepository vueloProgramadoRepository;

  public OcupacionReportService(
      InstanciaVueloRepository instanciaVueloRepository,
      ReservaAsientoRepository reservaAsientoRepository,
      VueloProgramadoRepository vueloProgramadoRepository) {
    this.instanciaVueloRepository = instanciaVueloRepository;
    this.reservaAsientoRepository = reservaAsientoRepository;
    this.vueloProgramadoRepository = vueloProgramadoRepository;
  }

  @Transactional(readOnly = true)
  public List<OcupacionVueloDto> generarReporte(
      Long vueloId, String clase, LocalDate fechaDesde, LocalDate fechaHasta) {
    if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
      throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta.");
    }

    final String claseNormalizada = normalizarClase(clase);
    final List<InstanciaVuelo> instancias =
        instanciaVueloRepository.buscarParaReporte(vueloId, fechaDesde, fechaHasta);
    if (instancias.isEmpty()) {
      return List.of();
    }

    final List<Long> instanciaIds = instancias.stream().map(InstanciaVuelo::getId).toList();
    final Map<Long, Map<String, Long>> vendidosPorInstancia = new HashMap<>();
    for (AsientosVendidosPorClase vendidos :
        reservaAsientoRepository.contarVendidosPorInstancias(
            instanciaIds, EstadoReservaAsiento.VENDIDO)) {
      vendidosPorInstancia
          .computeIfAbsent(vendidos.instanciaVueloId(), ignored -> new HashMap<>())
          .put(vendidos.clase().toUpperCase(Locale.ROOT), vendidos.vendidos());
    }

    final List<OcupacionVueloDto> reporte = new ArrayList<>();
    for (InstanciaVuelo instancia : instancias) {
      agregarClase(reporte, instancia, vendidosPorInstancia, ECONOMY, claseNormalizada);
      agregarClase(reporte, instancia, vendidosPorInstancia, PRIMERA, claseNormalizada);
    }
    return List.copyOf(reporte);
  }

  @Transactional(readOnly = true)
  public List<VueloProgramado> listarVuelos() {
    return vueloProgramadoRepository.findAllByOrderByCodigoVueloAsc();
  }

  public String normalizarClase(String clase) {
    if (clase == null || clase.isBlank()) {
      return TODAS_LAS_CLASES;
    }
    final String normalizada = clase.trim().toUpperCase(Locale.ROOT);
    if (!TODAS_LAS_CLASES.equals(normalizada)
        && !ECONOMY.equals(normalizada)
        && !PRIMERA.equals(normalizada)) {
      throw new IllegalArgumentException("La clase seleccionada no es válida.");
    }
    return normalizada;
  }

  private void agregarClase(
      List<OcupacionVueloDto> reporte,
      InstanciaVuelo instancia,
      Map<Long, Map<String, Long>> vendidosPorInstancia,
      String clase,
      String claseFiltro) {
    if (!TODAS_LAS_CLASES.equals(claseFiltro) && !claseFiltro.equals(clase)) {
      return;
    }

    final int capacidad =
        ECONOMY.equals(clase)
            ? instancia.getVueloProgramado().getAeronave().getEconomySeats()
            : instancia.getVueloProgramado().getAeronave().getPrimeraClaseSeats();
    final long vendidos =
        vendidosPorInstancia.getOrDefault(instancia.getId(), Map.of()).getOrDefault(clase, 0L);
    final BigDecimal porcentajeOcupacion =
        capacidad == 0
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(vendidos)
                .multiply(PORCENTAJE)
                .divide(BigDecimal.valueOf(capacidad), 2, RoundingMode.HALF_UP);

    reporte.add(
        new OcupacionVueloDto(
            instancia.getVueloProgramado().getCodigoVuelo(),
            instancia.getFechaSalida(),
            instancia.getVueloProgramado().getOrigen().getCodigoIata(),
            instancia.getVueloProgramado().getDestino().getCodigoIata(),
            clase,
            vendidos,
            capacidad,
            porcentajeOcupacion));
  }
}
