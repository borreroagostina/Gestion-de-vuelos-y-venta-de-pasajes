package com.aerolinea.gestionvuelos.service;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Orquesta la cancelacion de pasajes (US-16): registra la cancelacion, reembolsa por la pasarela y
 * avisa al pasajero. Si la pasarela no responde, el reembolso queda pendiente y se reintenta.
 */
@Service
public class TicketRefundService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TicketRefundService.class);

  private final TicketCancellationService cancellationService;
  private final PaymentGateway gateway;
  private final TicketMailService mailService;

  public TicketRefundService(
      TicketCancellationService cancellationService,
      PaymentGateway gateway,
      TicketMailService mailService) {
    this.cancellationService = cancellationService;
    this.gateway = gateway;
    this.mailService = mailService;
  }

  /** Cancela el pasaje e intenta el reembolso. Devuelve true si el reembolso ya se confirmó. */
  public CancellationOutcome cancelarPasaje(Long reservaAsientoId, String email) {
    final TicketCancellationService.CancellationResult result =
        cancellationService.cancelar(reservaAsientoId, email);
    final boolean refunded = procesarReembolso(result.ticketId());
    return new CancellationOutcome(result.refundAmount(), result.percentage(), refunded);
  }

  @Scheduled(fixedDelayString = "${app.payment.refund-retry-ms:60000}")
  public void reintentarReembolsosPendientes() {
    for (Long ticketId : cancellationService.idsReembolsosPendientes()) {
      procesarReembolso(ticketId);
    }
  }

  private boolean procesarReembolso(Long ticketId) {
    try {
      final var pending = cancellationService.obtenerReembolsoPendiente(ticketId);
      if (pending.isEmpty()) {
        return false;
      }
      final TicketCancellationService.RefundRequest request = pending.get();
      final boolean succeeded =
          gateway.refund(
              request.paymentIntentId(), request.amount(), "refund-ticket-" + request.ticketId());
      if (!succeeded) {
        LOGGER.warn("El reembolso del pasaje {} continúa pendiente en la pasarela.", ticketId);
        return false;
      }
      if (cancellationService.marcarReembolsado(ticketId)) {
        notificar(request);
      }
      return true;
    } catch (RuntimeException exception) {
      LOGGER.error(
          "No se pudo reembolsar el pasaje {}. Se reintentará automáticamente.",
          ticketId,
          exception);
      return false;
    }
  }

  private void notificar(TicketCancellationService.RefundRequest request) {
    try {
      mailService.enviarConfirmacionCancelacion(
          request.email(),
          request.flightCode(),
          request.seatNumber(),
          request.passengerName(),
          request.amount(),
          request.currency());
    } catch (RuntimeException exception) {
      LOGGER.error(
          "No se pudo enviar el email de cancelación del pasaje {}.",
          request.ticketId(),
          exception);
    }
  }

  public record CancellationOutcome(BigDecimal refundAmount, int percentage, boolean refunded) {}
}
