package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aerolinea.gestionvuelos.model.EmailOutboxStatus;
import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PurchaseEmailOutbox;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.PurchaseEmailOutboxRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.service.PdfGeneratorService;
import com.aerolinea.gestionvuelos.service.PurchaseEmailOutboxService;
import com.aerolinea.gestionvuelos.service.TicketMailService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PurchaseEmailOutboxServiceTest {

  private static final long RESERVATION_ID = 25L;

  @Mock private PurchaseEmailOutboxRepository outboxRepository;
  @Mock private ReservaAsientoRepository reservationSeatRepository;
  @Mock private PdfGeneratorService pdfGeneratorService;
  @Mock private TicketMailService ticketMailService;
  @InjectMocks private PurchaseEmailOutboxService outboxService;

  private PurchaseEmailOutbox outbox;
  private PaymentOrder order;

  @Before
  public void setUp() {
    outbox = new PurchaseEmailOutbox();
    outbox.setStatus(EmailOutboxStatus.PENDING);
    outbox.setAttempts(0);
    order = new PaymentOrder();
    order.setOrderCode("order-test");
    order.setUsuario(new Usuario());
    order.getUsuario().setEmail("passenger@example.test");
    final ReservaTemporal reservation = new ReservaTemporal();
    reservation.setId(RESERVATION_ID);
    order.setReservaTemporal(reservation);
    outbox.setPaymentOrder(order);
    when(outboxRepository.findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            any(), any(LocalDateTime.class)))
        .thenReturn(List.of(outbox));
    when(reservationSeatRepository.findByReservaTemporalIdOrderByAsientoNumeroAsc(RESERVATION_ID))
        .thenReturn(List.of());
    when(pdfGeneratorService.generarComprobantes(eq(order), any()))
        .thenReturn(Map.of("ticket.pdf", new byte[] {1}));
  }

  @Test
  public void failedEmailIsPersistentlyScheduledForRetry() {
    doThrow(new IllegalStateException("SMTP indisponible"))
        .when(ticketMailService)
        .enviarComprobantes(eq("passenger@example.test"), eq("order-test"), any());

    outboxService.enviarPendientes();

    assertEquals(EmailOutboxStatus.RETRY, outbox.getStatus());
    assertEquals(1, outbox.getAttempts());
    assertNotNull(outbox.getNextAttemptAt());
    assertEquals("SMTP indisponible", outbox.getLastError());
  }

  @Test
  public void successfulEmailMarksOutboxItemAsSent() {
    outboxService.enviarPendientes();

    assertEquals(EmailOutboxStatus.SENT, outbox.getStatus());
    assertEquals(0, outbox.getAttempts());
    assertNotNull(outbox.getSentAt());
    verify(ticketMailService)
        .enviarComprobantes(eq("passenger@example.test"), eq("order-test"), any());
  }
}
