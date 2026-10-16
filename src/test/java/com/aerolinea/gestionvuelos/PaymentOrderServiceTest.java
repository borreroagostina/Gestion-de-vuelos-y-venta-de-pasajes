package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.aerolinea.gestionvuelos.dto.PasajeroViajeForm;
import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
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
import com.aerolinea.gestionvuelos.service.FlightService;
import com.aerolinea.gestionvuelos.service.PassengerCheckoutService;
import com.aerolinea.gestionvuelos.service.PaymentGateway;
import com.aerolinea.gestionvuelos.service.PaymentOrderService;
import com.aerolinea.gestionvuelos.service.PdfGeneratorService;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
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
public class PaymentOrderServiceTest {

  private static final int ECONOMY_SEAT_COUNT = 6;
  private static final int FIRST_PASSENGER_BIRTH_YEAR = 1990;
  private static final int SECOND_PASSENGER_BIRTH_YEAR = 1988;
  private static final int FLIGHT_ARRIVAL_HOUR = 12;

  @Autowired private PaymentOrderService paymentOrderService;
  @Autowired private BookingService bookingService;
  @Autowired private PassengerCheckoutService passengerCheckoutService;
  @Autowired private FlightService flightService;
  @Autowired private AeropuertoRepository airportRepository;
  @Autowired private AeronaveRepository aircraftRepository;
  @Autowired private InstanciaVueloRepository flightInstanceRepository;
  @Autowired private AsientoRepository seatRepository;
  @Autowired private ReservaAsientoRepository seatReservationRepository;
  @Autowired private UsuarioRepository userRepository;
  @Autowired private PaymentOrderRepository orderRepository;
  @Autowired private PurchaseEmailOutboxRepository outboxRepository;
  @Autowired private PdfGeneratorService pdfGeneratorService;

  private Aeropuerto origin;
  private Aeropuerto destination;
  private Aeronave aircraft;
  private Usuario passenger;

  @Before
  public void setUp() {
    origin = airportRepository.save(new Aeropuerto("EZE", "Ezeiza", "Buenos Aires", "Argentina"));
    destination =
        airportRepository.save(new Aeropuerto("COR", "Pajas Blancas", "Córdoba", "Argentina"));
    aircraft =
        aircraftRepository.save(
            new Aeronave("Test Jet", "Test", "TJ" + System.nanoTime(), ECONOMY_SEAT_COUNT, 2));
    passenger = new Usuario();
    passenger.setEmail("payment-" + System.nanoTime() + "@test.example");
    passenger.setNombre("Test");
    passenger.setApellido("Passenger");
    passenger.setDocumento("DOC" + System.nanoTime());
    passenger.setTelefono("11223344");
    passenger.setPasswordHash("unused");
    passenger.setRol(RolUsuario.PASAJERO);
    passenger.setActivo(true);
    passenger = userRepository.save(passenger);
  }

  @Test
  @Transactional
  public void approvedPaymentSellsSeatsAndQueuesOneEmailWithIndividualTickets() throws IOException {
    final ReservaTemporal reservation = createReservation();
    final PaymentOrderService.PendingOrder order = createOrder(reservation);
    final PaymentGateway.GatewayPayment payment =
        payment(order, "approved", "tx-" + order.orderCode());

    assertEquals(PaymentOrderStatus.APPROVED, paymentOrderService.applyPayment(payment));
    assertEquals(PaymentOrderStatus.APPROVED, paymentOrderService.applyPayment(payment));

    final ReservaAsiento seatReservation =
        seatReservationRepository.findByReservaTemporalId(reservation.getId()).getFirst();
    assertEquals(EstadoReservaAsiento.VENDIDO, seatReservation.getEstado());
    assertEquals(
        "VENDIDO",
        seatRepository.findById(seatReservation.getAsiento().getId()).orElseThrow().getEstado());
    assertFalse(
        passengerCheckoutService.tieneReservaActiva(
            passenger.getEmail(), reservation.getSesionId()));
    assertEquals(
        ECONOMY_SEAT_COUNT - 2,
        bookingService.contarDisponibles(
            seatReservation.getAsiento().getInstanciaVuelo().getId(), "ECONOMY"));
    assertEquals(1, outboxRepository.count());

    final PaymentOrder savedOrder =
        orderRepository.findByOrderCode(order.orderCode()).orElseThrow();
    final Map<String, byte[]> attachments =
        pdfGeneratorService.generarComprobantes(
            savedOrder,
            seatReservationRepository.findByReservaTemporalIdOrderByAsientoNumeroAsc(
                reservation.getId()));
    assertEquals(3, attachments.size());
    assertEquals(
        2, attachments.keySet().stream().filter(name -> name.startsWith("Pasaje-")).count());
    assertTrue(
        attachments.containsKey("Pasaje-" + seatReservation.getAsiento().getNumero() + ".pdf"));
    assertTrue(attachments.keySet().stream().anyMatch(name -> name.startsWith("Factura-")));
    assertTrue(
        attachments.values().stream().allMatch(bytes -> new String(bytes, 0, 4).equals("%PDF")));
    final PdfReader ticketReader =
        new PdfReader(
            attachments.get("Pasaje-" + seatReservation.getAsiento().getNumero() + ".pdf"));
    final String ticketText = new PdfTextExtractor(ticketReader).getTextFromPage(1);
    assertTrue(ticketText.contains(order.orderCode()));
    assertTrue(ticketText.contains("Ana Pérez"));
    assertTrue(
        ticketReader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT).size()
            > 0);
    ticketReader.close();
    final String invoiceName =
        attachments.keySet().stream()
            .filter(name -> name.startsWith("Factura-"))
            .findFirst()
            .orElseThrow();
    final PdfReader invoiceReader = new PdfReader(attachments.get(invoiceName));
    final String invoiceText = new PdfTextExtractor(invoiceReader).getTextFromPage(1);
    assertTrue(invoiceText.contains("Subtotal: 2000.00 ARS"));
    assertTrue(invoiceText.contains("Impuestos: 420.00 ARS"));
    assertTrue(invoiceText.contains("Transacción: " + payment.id()));
    invoiceReader.close();
  }

  @Test
  @Transactional
  public void rejectedPaymentKeepsTheTemporarySeatHold() {
    final ReservaTemporal reservation = createReservation();
    final PaymentOrderService.PendingOrder order = createOrder(reservation);

    assertEquals(
        PaymentOrderStatus.REJECTED,
        paymentOrderService.applyPayment(payment(order, "rejected", "tx-rejected")));
    assertEquals(
        EstadoReservaAsiento.RESERVADO_TEMPORAL,
        seatReservationRepository
            .findByReservaTemporalId(reservation.getId())
            .getFirst()
            .getEstado());
    assertEquals(0, outboxRepository.count());
  }

  @Test
  @Transactional
  public void approvedPaymentAfterReservationExpiryIsQueuedForRefundNotSale() {
    final ReservaTemporal reservation = createReservation();
    final PaymentOrderService.PendingOrder order = createOrder(reservation);
    reservation.setFechaExpiracion(java.time.LocalDateTime.now().minusSeconds(1));

    assertEquals(
        PaymentOrderStatus.REFUND_PENDING,
        paymentOrderService.applyPayment(payment(order, "approved", "tx-late")));
    assertEquals(
        EstadoReservaAsiento.RESERVADO_TEMPORAL,
        seatReservationRepository
            .findByReservaTemporalId(reservation.getId())
            .getFirst()
            .getEstado());
    assertEquals(0, outboxRepository.count());
  }

  private PaymentOrderService.PendingOrder createOrder(ReservaTemporal reservation) {
    final PasajeroViajeForm form = new PasajeroViajeForm();
    form.setNombre("Ana");
    form.setApellido("Pérez");
    form.setTipoDocumento("DNI");
    form.setNumeroDocumento("12345678");
    form.setFechaNacimiento(LocalDate.of(FIRST_PASSENGER_BIRTH_YEAR, 1, 1));
    final PasajeroViajeForm secondPassenger = new PasajeroViajeForm();
    secondPassenger.setNombre("Luis");
    secondPassenger.setApellido("Gómez");
    secondPassenger.setTipoDocumento("DNI");
    secondPassenger.setNumeroDocumento("87654321");
    secondPassenger.setFechaNacimiento(LocalDate.of(SECOND_PASSENGER_BIRTH_YEAR, 4, 3));
    return paymentOrderService.createOrder(
        passenger.getId(), reservation.getId(), List.of(form, secondPassenger), "ARS");
  }

  private PaymentGateway.GatewayPayment payment(
      PaymentOrderService.PendingOrder order, String status, String transactionId) {
    return new PaymentGateway.GatewayPayment(
        transactionId, status, order.orderCode(), order.amount(), order.currency());
  }

  private ReservaTemporal createReservation() {
    final VueloProgramado flight = new VueloProgramado();
    flight.setCodigoVuelo("PAY" + System.nanoTime());
    flight.setOrigen(origin);
    flight.setDestino(destination);
    flight.setAeronave(aircraft);
    flight.setHoraSalida(LocalTime.of(10, 0));
    flight.setHoraLlegada(LocalTime.of(FLIGHT_ARRIVAL_HOUR, 0));
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
    final List<Long> seats =
        seatRepository.findByInstanciaVueloIdOrderByNumeroAsc(instance.getId()).stream()
            .filter(item -> "ECONOMY".equals(item.getClase()))
            .limit(2)
            .map(Asiento::getId)
            .toList();
    return bookingService.reservarAsientos(
        passenger.getId(), "payment-session-" + System.nanoTime(), seats, "ECONOMY");
  }
}
