package com.aerolinea.gestionvuelos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.EstadoReembolso;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.AsientoRepository;
import com.aerolinea.gestionvuelos.repository.PaymentOrderRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.service.PaymentGateway;
import com.aerolinea.gestionvuelos.service.TicketCancellationService;
import com.aerolinea.gestionvuelos.service.TicketMailService;
import com.aerolinea.gestionvuelos.service.TicketRefundService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketCancellationTest {

  private static final String EMAIL = "pasajero@test.com";
  private static final long TICKET_ID = 7L;
  private static final int EXPECTED_PERCENT = 80;

  @Mock private ReservaAsientoRepository reservaAsientoRepository;
  @Mock private PaymentOrderRepository paymentOrderRepository;
  @Mock private AsientoRepository asientoRepository;
  @Mock private PaymentGateway gateway;
  @Mock private TicketMailService mailService;

  private TicketCancellationService cancellationService;
  private TicketRefundService refundService;
  private ReservaAsiento ticket;
  private Asiento seat;
  private PaymentOrder order;

  @BeforeEach
  void setUp() {
    cancellationService =
        new TicketCancellationService(
            reservaAsientoRepository, paymentOrderRepository, asientoRepository);
    refundService = new TicketRefundService(cancellationService, gateway, mailService);

    final Usuario user = new Usuario();
    user.setEmail(EMAIL);
    final ReservaTemporal reservation = new ReservaTemporal();
    reservation.setId(5L);
    reservation.setUsuario(user);

    final InstanciaVuelo flight = new InstanciaVuelo();
    flight.setFechaSalida(LocalDate.now().plusDays(10));
    flight.setHoraSalida(java.time.LocalTime.NOON);

    seat = new Asiento();
    seat.setNumero("1A");
    seat.setClase("ECONOMY");
    seat.setEstado("VENDIDO");
    seat.setInstanciaVuelo(flight);

    ticket = new ReservaAsiento();
    ticket.setId(TICKET_ID);
    ticket.setReservaTemporal(reservation);
    ticket.setAsiento(seat);
    ticket.setEstado(EstadoReservaAsiento.VENDIDO);
    ticket.setNombrePasajero("Ana");
    ticket.setApellidoPasajero("Pérez");

    order = new PaymentOrder();
    order.setAmount(new BigDecimal("200.00"));
    order.setExternalTransactionId("pi_123");
    order.setCurrency("USD");
  }

  private void stubTicketAndOrder() {
    when(reservaAsientoRepository.findByIdWithLock(TICKET_ID)).thenReturn(Optional.of(ticket));
    when(paymentOrderRepository.findOwnedByReservaTemporalIdAndStatus(
            5L, EMAIL, PaymentOrderStatus.APPROVED))
        .thenReturn(Optional.of(order));
    when(reservaAsientoRepository.findByReservaTemporalId(5L)).thenReturn(List.of(ticket, ticket));
  }

  @Test
  void cancelFreesSeatAndLeavesRefundPending() {
    stubTicketAndOrder();

    final var result = cancellationService.cancelar(TICKET_ID, EMAIL);

    assertEquals(EXPECTED_PERCENT, result.percentage());
    assertEquals(new BigDecimal("80.00"), result.refundAmount());
    assertEquals(EstadoReservaAsiento.CANCELADO, ticket.getEstado());
    assertEquals(EstadoReembolso.PENDIENTE, ticket.getEstadoReembolso());
    assertEquals("DISPONIBLE", seat.getEstado());
  }

  @Test
  void cannotCancelOtherUsersTicket() {
    when(reservaAsientoRepository.findByIdWithLock(TICKET_ID)).thenReturn(Optional.of(ticket));

    assertThrows(
        IllegalArgumentException.class,
        () -> cancellationService.cancelar(TICKET_ID, "otro@test.com"));
    assertEquals(EstadoReservaAsiento.VENDIDO, ticket.getEstado());
  }

  @Test
  void cannotCancelAfterDeparture() {
    seat.getInstanciaVuelo().setFechaSalida(LocalDate.now().minusDays(1));
    when(reservaAsientoRepository.findByIdWithLock(TICKET_ID)).thenReturn(Optional.of(ticket));

    assertThrows(IllegalStateException.class, () -> cancellationService.cancelar(TICKET_ID, EMAIL));
  }

  @Test
  void cannotCancelTwice() {
    ticket.setEstado(EstadoReservaAsiento.CANCELADO);
    when(reservaAsientoRepository.findByIdWithLock(TICKET_ID)).thenReturn(Optional.of(ticket));

    assertThrows(IllegalStateException.class, () -> cancellationService.cancelar(TICKET_ID, EMAIL));
  }

  @Test
  void gatewayFailureKeepsRefundPendingWithoutEmail() {
    stubTicketAndOrder();
    when(reservaAsientoRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
    when(paymentOrderRepository.findFirstByReservaTemporalIdAndStatusOrderByIdDesc(
            5L, PaymentOrderStatus.APPROVED))
        .thenReturn(Optional.of(order));
    order.setUsuario(ticket.getReservaTemporal().getUsuario());
    order.setCurrency("USD");
    seat.getInstanciaVuelo()
        .setVueloProgramado(new com.aerolinea.gestionvuelos.model.VueloProgramado());
    seat.getInstanciaVuelo().getVueloProgramado().setCodigoVuelo("AR1");
    when(gateway.refund(anyString(), any(), anyString())).thenThrow(new IllegalStateException());

    final var outcome = refundService.cancelarPasaje(TICKET_ID, EMAIL);

    assertFalse(outcome.refunded());
    assertEquals(EstadoReembolso.PENDIENTE, ticket.getEstadoReembolso());
    verify(mailService, never())
        .enviarConfirmacionCancelacion(any(), any(), any(), any(), any(), any());
    assertTrue(outcome.refundAmount().signum() > 0);
    assertTrue(LocalDateTime.now().isAfter(ticket.getCanceladoEn().minusSeconds(1)));
  }

  @Test
  void retryDetectsRefundAlreadySucceededInStripeAndSendsEmail() {
    stubTicketAndOrder();
    when(reservaAsientoRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
    when(reservaAsientoRepository.findByEstadoReembolso(EstadoReembolso.PENDIENTE))
        .thenReturn(List.of(ticket));
    when(paymentOrderRepository.findFirstByReservaTemporalIdAndStatusOrderByIdDesc(
            5L, PaymentOrderStatus.APPROVED))
        .thenReturn(Optional.of(order));
    order.setUsuario(ticket.getReservaTemporal().getUsuario());
    seat.getInstanciaVuelo()
        .setVueloProgramado(new com.aerolinea.gestionvuelos.model.VueloProgramado());
    seat.getInstanciaVuelo().getVueloProgramado().setCodigoVuelo("AR1");
    cancellationService.cancelar(TICKET_ID, EMAIL);
    when(gateway.getSucceededRefundAmounts("pi_123")).thenReturn(List.of(new BigDecimal("80.00")));

    refundService.reintentarReembolsosPendientes();

    assertEquals(EstadoReembolso.COMPLETADO, ticket.getEstadoReembolso());
    verify(gateway, never()).refund(anyString(), any(), anyString());
    verify(mailService)
        .enviarConfirmacionCancelacion(
            EMAIL, "AR1", "1A", "Ana Pérez", new BigDecimal("80.00"), "USD");
  }

  @Test
  void stripeWebhookConfirmsMatchingPendingRefundAndSendsEmail() {
    stubTicketAndOrder();
    when(reservaAsientoRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
    when(reservaAsientoRepository.findByEstadoReembolso(EstadoReembolso.PENDIENTE))
        .thenReturn(List.of(ticket));
    when(paymentOrderRepository.findFirstByReservaTemporalIdAndStatusOrderByIdDesc(
            5L, PaymentOrderStatus.APPROVED))
        .thenReturn(Optional.of(order));
    order.setUsuario(ticket.getReservaTemporal().getUsuario());
    order.setCurrency("USD");
    seat.getInstanciaVuelo()
        .setVueloProgramado(new com.aerolinea.gestionvuelos.model.VueloProgramado());
    seat.getInstanciaVuelo().getVueloProgramado().setCodigoVuelo("AR1");
    cancellationService.cancelar(TICKET_ID, EMAIL);

    refundService.confirmarReembolsoExterno(
        new PaymentGateway.ConfirmedRefund("pi_123", new BigDecimal("80.00"), "USD"));

    assertEquals(EstadoReembolso.COMPLETADO, ticket.getEstadoReembolso());
    verify(mailService)
        .enviarConfirmacionCancelacion(
            EMAIL, "AR1", "1A", "Ana Pérez", new BigDecimal("80.00"), "USD");
  }
}
