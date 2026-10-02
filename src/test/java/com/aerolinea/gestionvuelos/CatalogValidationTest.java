package test.java.com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.repository.AeronaveRepository;
import com.aerolinea.gestionvuelos.repository.AeropuertoRepository;
import com.aerolinea.gestionvuelos.service.CatalogService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

@RunWith(MockitoJUnitRunner.class)
public class CatalogValidationTest {

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
    Aeropuerto aeropuerto = new Aeropuerto();
    aeropuerto.setNombre("Ezeiza");
    aeropuerto.setCiudad("Buenos Aires");
    aeropuerto.setPais("Argentina");

    assertThrows(IllegalArgumentException.class, () -> catalogService.guardarAeropuerto(aeropuerto));
  }

  @Test
  public void shouldRejectAircraftWithNegativeSeats() {
    Aeronave aeronave = new Aeronave();
    aeronave.setModelo("Airbus A320");
    aeronave.setFabricante("Airbus");
    aeronave.setCodigo("A320-01");
    aeronave.setEconomySeats(-5);
    aeronave.setPrimeraClaseSeats(10);

    assertThrows(IllegalArgumentException.class, () -> catalogService.guardarAeronave(aeronave));
  }

  @Test
  public void shouldRejectDeletingAirportWithFlights() {
    when(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM vuelos WHERE origen_aeropuerto_id = ? OR destino_aeropuerto_id = ?",
            Long.class,
            5L,
            5L))
        .thenReturn(1L);

    assertThrows(IllegalStateException.class, () -> catalogService.eliminarAeropuerto(5L));
  }

  @Test
  public void shouldRejectDeletingAircraftWithFlights() {
    when(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM vuelos WHERE aeronave_id = ?", Long.class, 7L))
        .thenReturn(1L);

    assertThrows(IllegalStateException.class, () -> catalogService.eliminarAeronave(7L));
  }
}
