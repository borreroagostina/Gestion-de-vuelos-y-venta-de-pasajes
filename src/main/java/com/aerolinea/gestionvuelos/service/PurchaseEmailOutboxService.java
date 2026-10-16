package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.EmailOutboxStatus;
import com.aerolinea.gestionvuelos.model.PurchaseEmailOutbox;
import com.aerolinea.gestionvuelos.repository.PurchaseEmailOutboxRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseEmailOutboxService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PurchaseEmailOutboxService.class);
  private static final int MAX_ERROR_LENGTH = 1000;
  private static final int MAX_BACKOFF_SHIFT = 9;
  private final PurchaseEmailOutboxRepository outboxRepository;
  private final ReservaAsientoRepository reservationSeatRepository;
  private final PdfGeneratorService pdfGeneratorService;
  private final TicketMailService ticketMailService;

  public PurchaseEmailOutboxService(
      PurchaseEmailOutboxRepository outboxRepository,
      ReservaAsientoRepository reservationSeatRepository,
      PdfGeneratorService pdfGeneratorService,
      TicketMailService ticketMailService) {
    this.outboxRepository = outboxRepository;
    this.reservationSeatRepository = reservationSeatRepository;
    this.pdfGeneratorService = pdfGeneratorService;
    this.ticketMailService = ticketMailService;
  }

  @Scheduled(fixedDelayString = "${app.email.outbox-poll-ms:30000}")
  @Transactional
  public void enviarPendientes() {
    final List<PurchaseEmailOutbox> due =
        outboxRepository.findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            List.of(EmailOutboxStatus.PENDING, EmailOutboxStatus.RETRY), LocalDateTime.now());
    for (PurchaseEmailOutbox item : due) {
      try {
        final var order = item.getPaymentOrder();
        final var passengers =
            reservationSeatRepository.findByReservaTemporalIdOrderByAsientoNumeroAsc(
                order.getReservaTemporal().getId());
        final var attachments = pdfGeneratorService.generarComprobantes(order, passengers);
        ticketMailService.enviarComprobantes(
            order.getUsuario().getEmail(), order.getOrderCode(), attachments);
        item.setStatus(EmailOutboxStatus.SENT);
        item.setSentAt(LocalDateTime.now());
        item.setLastError(null);
      } catch (RuntimeException exception) {
        final int attempts = item.getAttempts() + 1;
        final long delaySeconds =
            Math.min(60L * (1L << Math.min(attempts, MAX_BACKOFF_SHIFT)), 21600L);
        item.setAttempts(attempts);
        item.setStatus(EmailOutboxStatus.RETRY);
        item.setNextAttemptAt(LocalDateTime.now().plusSeconds(delaySeconds));
        final String error =
            exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        item.setLastError(error.substring(0, Math.min(MAX_ERROR_LENGTH, error.length())));
        LOGGER.error(
            "No se pudieron enviar los comprobantes de la orden {}. Reintento {} programado.",
            item.getPaymentOrder().getOrderCode(),
            attempts,
            exception);
      }
    }
  }
}
