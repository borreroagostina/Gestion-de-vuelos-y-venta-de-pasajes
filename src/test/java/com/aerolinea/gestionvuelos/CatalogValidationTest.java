package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.repository.AeronaveRepository;
import com.aerolinea.gestionvuelos.repository.AeropuertoRepository;
import com.aerolinea.gestionvuelos.service.CatalogService;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

@RunWith(MockitoJUnitRunner.class)
public class CatalogValidationTest {

  private static final Long AIRPORT_ID = 5L;
  private static final Long AIRCRAFT_ID = 7L;
  private static final int NEGATIVE_SEATS = -5;
  private static final int ECONOMY_SEATS = 180;
  private static final int FIRST_CLASS_SEATS = 20;
  private static final String AIRPORT_FLIGHT_COUNT_QUERY =
      "SELECT COUNT(*) FROM vuelos WHERE origen_aeropuerto_id = ? "
          + "OR destino_aeropuerto_id = ?";

  @Mock private AeropuertoRepository aeropuertoRepository;
  @Mock private AeronaveRepository aeronaveRepository;
  @Mock private JdbcTemplate jdbcTemplate;

  private CatalogService catalogService;

  @Before
  public void setUp() {
    catalogService = new CatalogService(aeropuertoRepository, aeronaveRepository, jdbcTemplate);
  }

  @Test
  public void shouldRejectAirportWithoutCode() {
    final Aeropuerto aeropuerto = new Aeropuerto();
    aeropuerto.setNombre("Ezeiza");
    aeropuerto.setCiudad("Buenos Aires");
    aeropuerto.setPais("Argentina");

    assertThrows(
        IllegalArgumentException.class, () -> catalogService.guardarAeropuerto(aeropuerto));
  }

  @Test
  public void shouldRejectDuplicateAirportCodeIgnoringCase() {
    final Aeropuerto aeropuerto = new Aeropuerto("eze", "Ezeiza", "Buenos Aires", "Argentina");
    when(aeropuertoRepository.existsByCodigoIataIgnoreCase("EZE")).thenReturn(true);

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class, () -> catalogService.guardarAeropuerto(aeropuerto));

    assertEquals("Ya existe un aeropuerto con ese código IATA", error.getMessage());
  }

  @Test
  public void shouldRejectAircraftWithNegativeSeats() {
    final Aeronave aeronave = new Aeronave();
    aeronave.setModelo("Airbus A320");
    aeronave.setFabricante("Airbus");
    aeronave.setCodigo("A320-01");
    aeronave.setEconomySeats(NEGATIVE_SEATS);
    aeronave.setPrimeraClaseSeats(10);

    assertThrows(IllegalArgumentException.class, () -> catalogService.guardarAeronave(aeronave));
  }

  @Test
  public void shouldRejectAircraftWithMissingSeatCounts() {
    final Aeronave aeronave = new Aeronave();
    aeronave.setModelo("Airbus A320");
    aeronave.setFabricante("Airbus");
    aeronave.setCodigo("A320-01");

    assertThrows(IllegalArgumentException.class, () -> catalogService.guardarAeronave(aeronave));
  }

  @Test
  public void shouldRejectDuplicateAircraftCodeIgnoringCase() {
    final Aeronave aeronave =
        new Aeronave("Airbus A320", "Airbus", "a320-01", ECONOMY_SEATS, FIRST_CLASS_SEATS);
    when(aeronaveRepository.existsByCodigoIgnoreCase("A320-01")).thenReturn(true);

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class, () -> catalogService.guardarAeronave(aeronave));

    assertEquals("Ya existe una aeronave con ese código", error.getMessage());
  }

  @Test
  public void shouldRejectUpdatingAirportWithAnotherAirportsCode() {
    final Aeropuerto aeropuerto = new Aeropuerto("EZE", "Aeropuerto", "Buenos Aires", "Argentina");
    aeropuerto.setId(1L);
    when(aeropuertoRepository.findById(1L)).thenReturn(Optional.of(aeropuerto));
    aeropuerto.setCodigoIata("AEP");
    when(aeropuertoRepository.existsByCodigoIataIgnoreCaseAndIdNot("AEP", 1L)).thenReturn(true);

    assertThrows(
        IllegalArgumentException.class, () -> catalogService.actualizarAeropuerto(aeropuerto));
  }

  @Test
  public void shouldRejectDeletingAirportWithFlights() {
    when(jdbcTemplate.queryForObject(
            AIRPORT_FLIGHT_COUNT_QUERY, Long.class, AIRPORT_ID, AIRPORT_ID))
        .thenReturn(1L);

    assertThrows(IllegalStateException.class, () -> catalogService.eliminarAeropuerto(AIRPORT_ID));
  }

  @Test
  public void shouldRejectDeletingAircraftWithFlights() {
    when(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM vuelos WHERE aeronave_id = ?", Long.class, AIRCRAFT_ID))
        .thenReturn(1L);

    assertThrows(IllegalStateException.class, () -> catalogService.eliminarAeronave(AIRCRAFT_ID));
  }
}
