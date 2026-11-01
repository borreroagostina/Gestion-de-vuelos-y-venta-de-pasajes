package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.EstadoReembolso;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.aerolinea.gestionvuelos.repository.AsientoRepository;
import com.aerolinea.gestionvuelos.repository.PaymentOrderRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operaciones transaccionales de cancelacion de pasajes (US-16). */
@Service
public class TicketCancellationService {

  private final ReservaAsientoRepository reservaAsientoRepository;
  private final PaymentOrderRepository paymentOrderRepository;
  private final AsientoRepository asientoRepository;

  public TicketCancellationService(
      ReservaAsientoRepository reservaAsientoRepository,
      PaymentOrderRepository paymentOrderRepository,
      AsientoRepository asientoRepository) {
    this.reservaAsientoRepository = reservaAsientoRepository;
    this.paymentOrderRepository = paymentOrderRepository;
    this.asientoRepository = asientoRepository;
  }

  /** Cancela un pasaje del usuario, libera el asiento y deja el reembolso pendiente de procesar. */
  @Transactional
  public CancellationResult cancelar(Long reservaAsientoId, String email) {
    final ReservaAsiento ticket =
        reservaAsientoRepository
            .findByIdWithLock(reservaAsientoId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró el pasaje."));
    if (!ticket.getReservaTemporal().getUsuario().getEmail().equalsIgnoreCase(email)) {
      throw new IllegalArgumentException("No tiene permisos para cancelar este pasaje.");
    }
    if (ticket.getEstado() != EstadoReservaAsiento.VENDIDO) {
      throw new IllegalStateException("El pasaje ya fue cancelado o no está confirmado.");
    }
    final Asiento seat = ticket.getAsiento();
    final InstanciaVuelo flight = seat.getInstanciaVuelo();
    final LocalDateTime departure = flight.getFechaSalida().atTime(flight.getHoraSalida());
    final LocalDateTime now = LocalDateTime.now();
    if (!departure.isAfter(now)) {
      throw new IllegalStateException("No se puede cancelar un pasaje de un vuelo que ya salió.");
    }

    final PaymentOrder order = findApprovedOrder(ticket, email);
    if (order.getPaymentMethod() != null
        || order.getExternalTransactionId() == null
        || order.getExternalTransactionId().isBlank()) {
      throw new IllegalStateException(
          "Los pasajes comprados en mostrador se cancelan en el mostrador.");
    }

    final int percentage =
        flight.isCancelada()
            ? RefundPolicy.FULL_PERCENT
            : RefundPolicy.percentage(seat.getClase(), Duration.between(now, departure));
    final BigDecimal refund = RefundPolicy.refundAmount(ticketPrice(order, ticket), percentage);

    ticket.setEstado(EstadoReservaAsiento.CANCELADO);
    ticket.setCanceladoEn(now);
    ticket.setMontoReembolso(refund);
    ticket.setEstadoReembolso(EstadoReembolso.PENDIENTE);
    seat.setEstado("DISPONIBLE");
    reservaAsientoRepository.save(ticket);
    asientoRepository.save(seat);
    return new CancellationResult(ticket.getId(), refund, percentage);
  }

  @Transactional(readOnly = true)
  public List<Long> idsReembolsosPendientes() {
    return reservaAsientoRepository.findByEstadoReembolso(EstadoReembolso.PENDIENTE).stream()
        .map(ReservaAsiento::getId)
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<RefundRequest> obtenerReembolsoPendiente(Long reservaAsientoId) {
    return reservaAsientoRepository
        .findById(reservaAsientoId)
        .filter(item -> item.getEstadoReembolso() == EstadoReembolso.PENDIENTE)
        .map(
            item -> {
              final PaymentOrder order =
                  paymentOrderRepository
                      .findByReservaTemporalId(item.getReservaTemporal().getId())
                      .orElseThrow(
                          () -> new IllegalStateException("No se encontró la orden de pago."));
              final InstanciaVuelo flight = item.getAsiento().getInstanciaVuelo();
              return new RefundRequest(
                  item.getId(),
                  order.getExternalTransactionId(),
                  item.getMontoReembolso(),
                  order.getCurrency(),
                  order.getUsuario().getEmail(),
                  flight.getVueloProgramado().getCodigoVuelo(),
                  item.getAsiento().getNumero(),
                  item.getNombrePasajero() + " " + item.getApellidoPasajero());
            });
  }

  @Transactional
  public boolean marcarReembolsado(Long reservaAsientoId) {
    return reservaAsientoRepository
        .findByIdWithLock(reservaAsientoId)
        .filter(item -> item.getEstadoReembolso() == EstadoReembolso.PENDIENTE)
        .map(
            item -> {
              item.setEstadoReembolso(EstadoReembolso.COMPLETADO);
              reservaAsientoRepository.save(item);
              return true;
            })
        .orElse(false);
  }

  private PaymentOrder findApprovedOrder(ReservaAsiento ticket, String email) {
    return paymentOrderRepository
        .findOwnedByReservaTemporalIdAndStatus(
            ticket.getReservaTemporal().getId(), email, PaymentOrderStatus.APPROVED)
        .orElseThrow(
            () -> new IllegalStateException("No se encontró la compra asociada al pasaje."));
  }

  private BigDecimal ticketPrice(PaymentOrder order, ReservaAsiento ticket) {
    final int tickets =
        Math.max(
            reservaAsientoRepository
                .findByReservaTemporalId(ticket.getReservaTemporal().getId())
                .size(),
            1);
    return order.getAmount().divide(BigDecimal.valueOf(tickets), 2, RoundingMode.HALF_UP);
  }

  public record CancellationResult(Long ticketId, BigDecimal refundAmount, int percentage) {}

  public record RefundRequest(
      Long ticketId,
      String paymentIntentId,
      BigDecimal amount,
      String currency,
      String email,
      String flightCode,
      String seatNumber,
      String passengerName) {}
}
