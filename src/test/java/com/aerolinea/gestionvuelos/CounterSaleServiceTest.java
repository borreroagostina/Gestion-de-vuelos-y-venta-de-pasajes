package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.aerolinea.gestionvuelos.dto.CounterSaleForm;
import com.aerolinea.gestionvuelos.dto.PasajeroViajeForm;
import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.EmailOutboxStatus;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.MetodoPagoMostrador;
import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import com.aerolinea.gestionvuelos.model.PurchaseEmailOutbox;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.model.VueloProgramado;
import com.aerolinea.gestionvuelos.repository.AeronaveRepository;
import com.aerolinea.gestionvuelos.repository.AeropuertoRepository;
import com.aerolinea.gestionvuelos.repository.AsientoRepository;
import com.aerolinea.gestionvuelos.repository.InstanciaVueloRepository;
import com.aerolinea.gestionvuelos.repository.PaymentOrderRepository;
import com.aerolinea.gestionvuelos.repository.PurchaseEmailOutboxRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.BookingService;
import com.aerolinea.gestionvuelos.service.CounterSaleService;
import com.aerolinea.gestionvuelos.service.CounterSaleService.CounterSaleResult;
import com.aerolinea.gestionvuelos.service.FlightService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
public class CounterSaleServiceTest {

  private static final int ECONOMY_SEATS = 12;
  private static final int MAX_SEATS = 9;
  private static final int ARRIVAL_HOUR = 12;
  private static final int BIRTH_YEAR = 1990;
  private static final long SUFFIX_MODULO = 100_000_000L;

  @Autowired private CounterSaleService counterSaleService;
  @Autowired private BookingService bookingService;
  @Autowired private FlightService flightService;
  @Autowired private AeropuertoRepository airportRepository;
  @Autowired private AeronaveRepository aircraftRepository;
  @Autowired private InstanciaVueloRepository flightInstanceRepository;
  @Autowired private AsientoRepository seatRepository;
  @Autowired private ReservaAsientoRepository seatReservationRepository;
  @Autowired private UsuarioRepository userRepository;
  @Autowired private PaymentOrderRepository orderRepository;
  @Autowired private PurchaseEmailOutboxRepository outboxRepository;

  private Usuario employee;
  private List<Long> economySeatIds;

  @Before
  public void setUp() {
    final String suffix = String.valueOf(System.nanoTime() % SUFFIX_MODULO);
    final Aeropuerto origin =
        airportRepository.save(new Aeropuerto("M" + suffix, "Origen", "Ciudad A", "Argentina"));
    final Aeropuerto destination =
        airportRepository.save(new Aeropuerto("N" + suffix, "Destino", "Ciudad B", "Argentina"));
    final Aeronave aircraft =
        aircraftRepository.save(
            new Aeronave("Test Jet", "Test", "CS" + System.nanoTime(), ECONOMY_SEATS, 2));
    employee = saveUser("empleado-" + System.nanoTime() + "@test.example", RolUsuario.EMPLEADO);

    final VueloProgramado flight = new VueloProgramado();
    flight.setCodigoVuelo("CS" + System.nanoTime());
    flight.setOrigen(origin);
    flight.setDestino(destination);
    flight.setAeronave(aircraft);
    flight.setHoraSalida(LocalTime.of(10, 0));
    flight.setHoraLlegada(LocalTime.of(ARRIVAL_HOUR, 0));
    flight.setFechaInicioVenta(LocalDate.now().plusDays(1));
    flight.setFechaFinVenta(LocalDate.now().plusDays(2));
    flight.setDiasSemana("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
    flight.setPrecioEconomy(new BigDecimal("1000.00"));
    flight.setPrecioPrimera(new BigDecimal("2000.00"));
    final VueloProgramado savedFlight = flightService.crearVuelo(flight);
    final InstanciaVuelo instance =
        flightInstanceRepository
            .findByVueloProgramadoIdOrderByFechaSalidaAsc(savedFlight.getId())
            .getFirst();
    economySeatIds =
        seatRepository.findByInstanciaVueloIdOrderByNumeroAsc(instance.getId()).stream()
            .filter(seat -> "ECONOMY".equals(seat.getClase()))
            .map(Asiento::getId)
            .toList();
  }

  @Test
  @Transactional
  public void sellsSeatsAtTheCounterRecordingEmployeeAndQueuingEmailToCustomer() {
    final ReservaTemporal reservation = hold(employee, economySeatIds.subList(0, 2));

    final CounterSaleResult result =
        counterSaleService.registerSale(
            employee.getId(), reservation.getId(), form("nuevo@test.example", "30111222", 2));

    final PaymentOrder order = orderRepository.findByOrderCode(result.orderCode()).orElseThrow();
    assertEquals(PaymentOrderStatus.APPROVED, order.getStatus());
    assertEquals(employee.getId(), order.getEmpleado().getId());
    assertEquals(MetodoPagoMostrador.EFECTIVO, order.getPaymentMethod());
    assertEquals("nuevo@test.example", order.getUsuario().getEmail());
    assertEquals(RolUsuario.PASAJERO, order.getUsuario().getRol());
    assertFalse(order.getUsuario().getActivo());
    assertEquals(0, new BigDecimal("2420.00").compareTo(order.getAmount()));
    assertTrue(
        seatReservationRepository.findByReservaTemporalId(reservation.getId()).stream()
            .allMatch(item -> item.getEstado() == EstadoReservaAsiento.VENDIDO));
    final PurchaseEmailOutbox outbox =
        outboxRepository.findAll().stream()
            .filter(item -> item.getPaymentOrder().getId().equals(order.getId()))
            .findFirst()
            .orElseThrow();
    assertEquals(EmailOutboxStatus.PENDING, outbox.getStatus());
  }

  @Test
  @Transactional
  public void reusesExistingPassengerAccountWhenEmailAndDocumentMatch() {
    final Usuario customer =
        saveUser("cliente-" + System.nanoTime() + "@test.example", RolUsuario.PASAJERO);
    final ReservaTemporal reservation = hold(employee, economySeatIds.subList(0, 1));

    final CounterSaleResult result =
        counterSaleService.registerSale(
            employee.getId(),
            reservation.getId(),
            form(customer.getEmail(), customer.getDocumento(), 1));

    assertEquals(
        customer.getId(),
        orderRepository.findByOrderCode(result.orderCode()).orElseThrow().getUsuario().getId());
  }

  @Test
  @Transactional
  public void rejectsExistingEmailWithDifferentDocument() {
    final Usuario customer =
        saveUser("otro-" + System.nanoTime() + "@test.example", RolUsuario.PASAJERO);
    final ReservaTemporal reservation = hold(employee, economySeatIds.subList(0, 1));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            counterSaleService.registerSale(
                employee.getId(), reservation.getId(), form(customer.getEmail(), "99999999", 1)));
  }

  @Test
  @Transactional
  public void passengersCannotRegisterCounterSales() {
    final Usuario passenger =
        saveUser("pax-" + System.nanoTime() + "@test.example", RolUsuario.PASAJERO);
    final ReservaTemporal reservation = hold(passenger, economySeatIds.subList(0, 1));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            counterSaleService.registerSale(
                passenger.getId(), reservation.getId(), form("x@test.example", "30111222", 1)));
  }

  @Test
  @Transactional
  public void keepsTheNineSeatLimitAndNeverSellsATakenSeatTwice() {
    assertThrows(
        IllegalArgumentException.class,
        () -> hold(employee, economySeatIds.subList(0, MAX_SEATS + 1)));
    assertEquals(
        MAX_SEATS, hold(employee, economySeatIds.subList(0, MAX_SEATS)).getAsientos().size());
    assertThrows(IllegalStateException.class, () -> hold(employee, economySeatIds.subList(0, 1)));
  }

  private ReservaTemporal hold(Usuario user, List<Long> seatIds) {
    return bookingService.reservarAsientos(
        user.getId(), "session-" + System.nanoTime(), seatIds, "ECONOMY");
  }

  private CounterSaleForm form(String email, String document, int passengers) {
    final CounterSaleForm form = new CounterSaleForm();
    form.setClienteNombre("Cliente");
    form.setClienteApellido("Mostrador");
    form.setClienteDocumento(document);
    form.setClienteEmail(email);
    form.setClienteTelefono("1122334455");
    form.setMetodoPago(MetodoPagoMostrador.EFECTIVO);
    final List<PasajeroViajeForm> list = new ArrayList<>();
    for (int i = 0; i < passengers; i++) {
      final PasajeroViajeForm passenger = new PasajeroViajeForm();
      passenger.setNombre("Ana" + i);
      passenger.setApellido("Pérez");
      passenger.setTipoDocumento("DNI");
      passenger.setNumeroDocumento("1234567" + i);
      passenger.setFechaNacimiento(LocalDate.of(BIRTH_YEAR, 1, 1));
      list.add(passenger);
    }
    form.setPasajeros(list);
    return form;
  }

  private Usuario saveUser(String email, RolUsuario role) {
    final Usuario user = new Usuario();
    user.setEmail(email);
    user.setNombre("Test");
    user.setApellido("User");
    user.setDocumento("DOC" + System.nanoTime());
    user.setTelefono("11223344");
    user.setPasswordHash("unused");
    user.setRol(role);
    user.setActivo(true);
    return userRepository.save(user);
  }
}
