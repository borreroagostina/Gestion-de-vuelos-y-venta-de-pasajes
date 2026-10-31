package com.aerolinea.gestionvuelos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aerolinea.gestionvuelos.dto.AsientosVendidosPorClase;
import com.aerolinea.gestionvuelos.dto.OcupacionVueloDto;
import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.VueloProgramado;
import com.aerolinea.gestionvuelos.repository.InstanciaVueloRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.repository.VueloProgramadoRepository;
import com.aerolinea.gestionvuelos.service.OcupacionReportService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OcupacionReportServiceTest {

  private static final long VENDIDOS_ECONOMY = 25L;
  private static final long REPORTE_FLIGHT_ID = 12L;
  private static final long REPORTE_INSTANCE_ID = 7L;
  private static final LocalDate FECHA_DESDE = LocalDate.parse("2026-10-01");
  private static final LocalDate FECHA_HASTA = LocalDate.parse("2026-10-31");
  private static final LocalDate FECHA_VUELO = LocalDate.parse("2026-10-15");

  private InstanciaVueloRepository instanciaRepository;
  private ReservaAsientoRepository reservaRepository;
  private VueloProgramadoRepository vueloRepository;
  private OcupacionReportService service;

  @BeforeEach
  void setUp() {
    instanciaRepository = mock(InstanciaVueloRepository.class);
    reservaRepository = mock(ReservaAsientoRepository.class);
    vueloRepository = mock(VueloProgramadoRepository.class);
    service = new OcupacionReportService(instanciaRepository, reservaRepository, vueloRepository);
  }

  @Test
  void calculatesOccupancyForEachClassFromAircraftCapacity() {
    final InstanciaVuelo instancia = instancia(1L, "AR101", 100, 20);
    when(instanciaRepository.buscarParaReporte(null, null, null)).thenReturn(List.of(instancia));
    when(reservaRepository.contarVendidosPorInstancias(List.of(1L), EstadoReservaAsiento.VENDIDO))
        .thenReturn(
            List.of(
                new AsientosVendidosPorClase(1L, "ECONOMY", VENDIDOS_ECONOMY),
                new AsientosVendidosPorClase(1L, "PRIMERA", 5L)));

    final List<OcupacionVueloDto> reporte = service.generarReporte(null, null, null, null);

    assertEquals(2, reporte.size());
    assertEquals("ECONOMY", reporte.get(0).clase());
    assertEquals(VENDIDOS_ECONOMY, reporte.get(0).vendidos());
    assertEquals(100, reporte.get(0).capacidad());
    assertEquals("25.00", reporte.get(0).porcentajeOcupacion().toPlainString());
    assertEquals("PRIMERA", reporte.get(1).clase());
    assertEquals("25.00", reporte.get(1).porcentajeOcupacion().toPlainString());
  }

  @Test
  void appliesFlightClassAndDateFiltersAndReturnsOnlySelectedClass() {
    final InstanciaVuelo instancia = instancia(REPORTE_INSTANCE_ID, "AR202", 0, 20);
    when(instanciaRepository.buscarParaReporte(REPORTE_FLIGHT_ID, FECHA_DESDE, FECHA_HASTA))
        .thenReturn(List.of(instancia));
    when(reservaRepository.contarVendidosPorInstancias(
            List.of(REPORTE_INSTANCE_ID), EstadoReservaAsiento.VENDIDO))
        .thenReturn(List.of(new AsientosVendidosPorClase(REPORTE_INSTANCE_ID, "PRIMERA", 4L)));

    final List<OcupacionVueloDto> reporte =
        service.generarReporte(REPORTE_FLIGHT_ID, "primera", FECHA_DESDE, FECHA_HASTA);

    assertEquals(1, reporte.size());
    assertEquals("PRIMERA", reporte.get(0).clase());
    assertEquals("20.00", reporte.get(0).porcentajeOcupacion().toPlainString());
    verify(instanciaRepository).buscarParaReporte(REPORTE_FLIGHT_ID, FECHA_DESDE, FECHA_HASTA);
  }

  @Test
  void returnsZeroOccupancyForClassWithoutCapacity() {
    final InstanciaVuelo instancia = instancia(3L, "AR303", 0, 0);
    when(instanciaRepository.buscarParaReporte(null, null, null)).thenReturn(List.of(instancia));
    when(reservaRepository.contarVendidosPorInstancias(List.of(3L), EstadoReservaAsiento.VENDIDO))
        .thenReturn(List.of());

    final List<OcupacionVueloDto> reporte = service.generarReporte(null, "ECONOMY", null, null);

    assertEquals(1, reporte.size());
    assertEquals(0, reporte.get(0).capacidad());
    assertEquals("0.00", reporte.get(0).porcentajeOcupacion().toPlainString());
  }

  @Test
  void rejectsInvalidDateRangeAndClass() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.generarReporte(
                null, "TODAS", LocalDate.parse("2026-11-01"), LocalDate.parse("2026-10-31")));
    assertThrows(
        IllegalArgumentException.class, () -> service.generarReporte(null, "BUSINESS", null, null));
  }

  private InstanciaVuelo instancia(Long id, String codigo, int economySeats, int primeraSeats) {
    final Aeropuerto origen = new Aeropuerto("EZE", "Ezeiza", "Buenos Aires", "Argentina");
    final Aeropuerto destino = new Aeropuerto("COR", "Pajas Blancas", "Córdoba", "Argentina");
    final Aeronave aeronave =
        new Aeronave("Modelo", "Fabricante", "AV" + codigo, economySeats, primeraSeats);
    final VueloProgramado vuelo = new VueloProgramado();
    vuelo.setCodigoVuelo(codigo);
    vuelo.setOrigen(origen);
    vuelo.setDestino(destino);
    vuelo.setAeronave(aeronave);

    final InstanciaVuelo instancia = new InstanciaVuelo();
    instancia.setId(id);
    instancia.setVueloProgramado(vuelo);
    instancia.setFechaSalida(FECHA_VUELO);
    return instancia;
  }
}
