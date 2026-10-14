package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.aerolinea.gestionvuelos.dto.DatosPasajeroForm;
import com.aerolinea.gestionvuelos.dto.DatosPasajerosForm;
import com.aerolinea.gestionvuelos.dto.ResumenCompraView;
import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.model.VueloProgramado;
import com.aerolinea.gestionvuelos.repository.AeronaveRepository;
import com.aerolinea.gestionvuelos.repository.AeropuertoRepository;
import com.aerolinea.gestionvuelos.repository.AsientoRepository;
import com.aerolinea.gestionvuelos.repository.InstanciaVueloRepository;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.BookingService;
import com.aerolinea.gestionvuelos.service.FlightService;
import com.aerolinea.gestionvuelos.service.PassengerCheckoutService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class PassengerCheckoutServiceTest {

  private static final int BOEING_SEATS_ECO = 150;
  private static final int BOEING_SEATS_PRI = 20;
  private static final int HORA_LLEGADA_12 = 12;
  private static final int FIRST_BIRTH_YEAR = 1990;
  private static final int SECOND_BIRTH_YEAR = 1988;

  @Autowired private PassengerCheckoutService passengerCheckoutService;

  @Autowired private BookingService bookingService;

  @Autowired private FlightService flightService;

  @Autowired private AeropuertoRepository aeropuertoRepository;

  @Autowired private AeronaveRepository aeronaveRepository;

  @Autowired private InstanciaVueloRepository instanciaVueloRepository;

  @Autowired private AsientoRepository asientoRepository;

  @Autowired private UsuarioRepository usuarioRepository;

  private Aeropuerto eze;
  private Aeropuerto cor;
  private Aeronave boeing;
  private Usuario pasajero;

  @Before
  public void setUp() {
    if (aeropuertoRepository.count() == 0) {
      eze = aeropuertoRepository.save(new Aeropuerto("EZE", "Ezeiza", "Buenos Aires", "Argentina"));
      cor =
          aeropuertoRepository.save(new Aeropuerto("COR", "Pajas Blancas", "Córdoba", "Argentina"));
    } else {
      eze = aeropuertoRepository.findAll().get(0);
      cor = aeropuertoRepository.findAll().get(1);
    }

    if (aeronaveRepository.count() == 0) {
      boeing =
          aeronaveRepository.save(
              new Aeronave("Boeing 737", "Boeing", "B737", BOEING_SEATS_ECO, BOEING_SEATS_PRI));
    } else {
      boeing = aeronaveRepository.findAll().get(0);
    }

    pasajero = new Usuario();
    pasajero.setEmail("checkout@test.com");
    pasajero.setNombre("Checkout");
    pasajero.setApellido("User");
    pasajero.setDocumento("99887766");
    pasajero.setTelefono("1122334455");
    pasajero.setPasswordHash("pass");
    pasajero.setRol(RolUsuario.PASAJERO);
    pasajero.setActivo(true);
    if (!usuarioRepository.existsByEmailIgnoreCase(pasajero.getEmail())) {
      pasajero = usuarioRepository.save(pasajero);
    } else {
      pasajero = usuarioRepository.findByEmailIgnoreCase(pasajero.getEmail()).orElseThrow();
    }
  }

  @Test
  @Transactional
  public void shouldCreateDynamicPassengerFormAndCalculateSummary() {
    final ReservaTemporal reserva = crearReservaConDosAsientos();

    final DatosPasajerosForm formulario =
        passengerCheckoutService.crearFormulario(pasajero.getEmail(), reserva.getSesionId());

    assertEquals(2, formulario.getPasajeros().size());
    assertEquals("07A", formulario.getPasajeros().get(0).getNumeroAsiento());
    assertEquals("07B", formulario.getPasajeros().get(1).getNumeroAsiento());

    final DatosPasajeroForm primero = formulario.getPasajeros().get(0);
    primero.setNombre("Ana");
    primero.setApellido("Pérez");
    primero.setDocumento("ABC12345");
    primero.setFechaNacimiento(LocalDate.of(FIRST_BIRTH_YEAR, 1, 10));

    final DatosPasajeroForm segundo = formulario.getPasajeros().get(1);
    segundo.setNombre("Luis");
    segundo.setApellido("Gómez");
    segundo.setDocumento("DNI998877");
    segundo.setFechaNacimiento(LocalDate.of(SECOND_BIRTH_YEAR, 4, 3));

    passengerCheckoutService.guardarPasajeros(
        pasajero.getEmail(), reserva.getSesionId(), formulario);

    final ResumenCompraView resumen =
        passengerCheckoutService.obtenerResumen(pasajero.getEmail(), reserva.getSesionId());

    assertEquals("Economy", resumen.getClase());
    assertEquals(new BigDecimal("1000.00"), resumen.getPrecioUnitario());
    assertEquals(new BigDecimal("2000.00"), resumen.getSubtotal());
    assertEquals(new BigDecimal("420.00"), resumen.getImpuestos());
    assertEquals(new BigDecimal("2420.00"), resumen.getTotal());
    assertEquals(2, resumen.getPasajeros().size());
    assertEquals(List.of("07A", "07B"), resumen.getAsientos());
  }

  @Test
  @Transactional
  public void shouldRequireAllPassengerDataBeforeSummary() {
    final ReservaTemporal reserva = crearReservaConDosAsientos();

    try {
      passengerCheckoutService.obtenerResumen(pasajero.getEmail(), reserva.getSesionId());
      fail("Debería exigir los datos completos de los pasajeros");
    } catch (IllegalStateException ex) {
      assertTrue(ex.getMessage().contains("Debe completar los datos de todos los pasajeros"));
    }
  }

  private ReservaTemporal crearReservaConDosAsientos() {
    final VueloProgramado vuelo = crearVueloGenerico("CHK" + System.nanoTime());
    final InstanciaVuelo instancia =
        instanciaVueloRepository.findByVueloProgramadoIdOrderByFechaSalidaAsc(vuelo.getId()).get(0);
    final List<Asiento> asientos =
        asientoRepository.findByInstanciaVueloIdOrderByNumeroAsc(instancia.getId());
    final List<Long> ids =
        asientos.stream()
            .filter(asiento -> "ECONOMY".equals(asiento.getClase()))
            .limit(2)
            .map(Asiento::getId)
            .toList();
    return bookingService.reservarAsientos(
        pasajero.getId(), "checkout-session-" + System.nanoTime(), ids, "ECONOMY");
  }

  private VueloProgramado crearVueloGenerico(String codigo) {
    final VueloProgramado vuelo = new VueloProgramado();
    vuelo.setCodigoVuelo(codigo);
    vuelo.setOrigen(eze);
    vuelo.setDestino(cor);
    vuelo.setAeronave(boeing);
    vuelo.setHoraSalida(LocalTime.of(10, 0));
    vuelo.setHoraLlegada(LocalTime.of(HORA_LLEGADA_12, 0));
    vuelo.setFechaInicioVenta(LocalDate.now().plusDays(1));
    vuelo.setFechaFinVenta(LocalDate.now().plusDays(2));
    vuelo.setDiasSemana("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
    vuelo.setPrecioEconomy(new BigDecimal("1000.00"));
    vuelo.setPrecioPrimera(new BigDecimal("2000.00"));
    return flightService.crearVuelo(vuelo);
  }
}
