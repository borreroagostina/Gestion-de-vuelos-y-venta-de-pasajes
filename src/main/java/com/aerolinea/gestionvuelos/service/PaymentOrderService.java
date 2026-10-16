package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.dto.PasajeroViajeForm;
import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.EmailOutboxStatus;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import com.aerolinea.gestionvuelos.model.PurchaseEmailOutbox;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.AsientoRepository;
import com.aerolinea.gestionvuelos.repository.PaymentOrderRepository;
import com.aerolinea.gestionvuelos.repository.PurchaseEmailOutboxRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.repository.ReservaTemporalRepository;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOrderService {

  private static final BigDecimal TAX_RATE = new BigDecimal("0.21");
  private static final String FIRST_CLASS = "PRIMERA";
  private final PaymentOrderRepository orderRepository;
  private final AsientoRepository seatRepository;
  private final ReservaTemporalRepository reservationRepository;
  private final ReservaAsientoRepository reservationSeatRepository;
  private final UsuarioRepository userRepository;
  private final PurchaseEmailOutboxRepository outboxRepository;

  public PaymentOrderService(
      PaymentOrderRepository orderRepository,
      AsientoRepository seatRepository,
      ReservaTemporalRepository reservationRepository,
      ReservaAsientoRepository reservationSeatRepository,
      UsuarioRepository userRepository,
      PurchaseEmailOutboxRepository outboxRepository) {
    this.orderRepository = orderRepository;
    this.seatRepository = seatRepository;
    this.reservationRepository = reservationRepository;
    this.reservationSeatRepository = reservationSeatRepository;
    this.userRepository = userRepository;
    this.outboxRepository = outboxRepository;
  }

  @Transactional
  public PendingOrder createOrder(
      Long userId, Long reservationId, List<PasajeroViajeForm> passengers, String currency) {
    final Usuario user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    final ReservaTemporal reservation =
        reservationRepository
            .findByIdWithLock(reservationId)
            .orElseThrow(() -> new IllegalStateException("La reserva temporal ya no existe."));
    if (!reservation.getUsuario().getId().equals(userId)) {
      throw new IllegalArgumentException("La reserva no pertenece al usuario autenticado.");
    }
    if (!reservation.getFechaExpiracion().isAfter(LocalDateTime.now())) {
      throw new IllegalStateException(
          "La reserva temporal expiró. Seleccioná los asientos nuevamente.");
    }

    final List<ReservaAsiento> reservationSeats =
        reservationSeatRepository.findByReservaTemporalIdWithLock(reservationId);
    if (reservationSeats.isEmpty()
        || passengers == null
        || passengers.size() != reservationSeats.size()) {
      throw new IllegalArgumentException(
          "Los datos de pasajeros no coinciden con los asientos reservados.");
    }
    reservationSeats.sort(Comparator.comparing(item -> item.getAsiento().getNumero()));
    for (int index = 0; index < reservationSeats.size(); index++) {
      final ReservaAsiento seatReservation = reservationSeats.get(index);
      final PasajeroViajeForm passenger = passengers.get(index);
      if (seatReservation.getEstado() != EstadoReservaAsiento.RESERVADO_TEMPORAL
          || !"RESERVADO".equals(seatReservation.getAsiento().getEstado())) {
        throw new IllegalStateException("Uno o más asientos ya no están reservados.");
      }
      if (passenger == null
          || passenger.getNombre() == null
          || passenger.getNombre().isBlank()
          || passenger.getApellido() == null
          || passenger.getApellido().isBlank()
          || passenger.getNumeroDocumento() == null
          || passenger.getNumeroDocumento().isBlank()
          || passenger.getFechaNacimiento() == null
          || !passenger.getFechaNacimiento().isBefore(LocalDate.now())) {
        throw new IllegalArgumentException("Los datos de los pasajeros están incompletos.");
      }
      seatReservation.setNombrePasajero(passenger.getNombre().trim());
      seatReservation.setApellidoPasajero(passenger.getApellido().trim());
      seatReservation.setDocumentoPasajero(passenger.getNumeroDocumento().trim().toUpperCase());
      seatReservation.setFechaNacimientoPasajero(passenger.getFechaNacimiento());
    }
    reservationSeatRepository.saveAll(reservationSeats);

    final Asiento firstSeat = reservationSeats.get(0).getAsiento();
    final BigDecimal unitPrice =
        FIRST_CLASS.equals(firstSeat.getClase())
            ? firstSeat.getInstanciaVuelo().getPrecioPrimera()
            : firstSeat.getInstanciaVuelo().getPrecioEconomy();
    final BigDecimal subtotal =
        unitPrice
            .multiply(BigDecimal.valueOf(reservationSeats.size()))
            .setScale(2, RoundingMode.HALF_UP);
    final BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    final BigDecimal total = subtotal.add(taxAmount);

    final PaymentOrder order = new PaymentOrder();
    order.setOrderCode(UUID.randomUUID().toString());
    order.setReservaTemporal(reservation);
    order.setUsuario(user);
    order.setSubtotal(subtotal);
    order.setTaxAmount(taxAmount);
    order.setAmount(total);
    order.setCurrency(currency);
    order.setStatus(PaymentOrderStatus.PENDING);
    order.setCreatedAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
    orderRepository.save(order);
    return new PendingOrder(
        order.getOrderCode(),
        order.getAmount(),
        order.getCurrency(),
        user.getEmail(),
        reservation.getFechaExpiracion());
  }

  @Transactional
  public PendingOrder createOrderFromSavedPassengerData(
      Long userId, Long reservationId, String currency) {
    final List<PasajeroViajeForm> passengers =
        reservationSeatRepository
            .findByReservaTemporalIdOrderByAsientoNumeroAsc(reservationId)
            .stream()
            .map(
                reservationSeat -> {
                  final PasajeroViajeForm passenger = new PasajeroViajeForm();
                  passenger.setNombre(reservationSeat.getNombrePasajero());
                  passenger.setApellido(reservationSeat.getApellidoPasajero());
                  passenger.setTipoDocumento(
                      reservationSeat.getDocumentoPasajero() != null
                              && reservationSeat.getDocumentoPasajero().matches("\\d{7,8}")
                          ? "DNI"
                          : "PASAPORTE");
                  passenger.setNumeroDocumento(reservationSeat.getDocumentoPasajero());
                  passenger.setFechaNacimiento(reservationSeat.getFechaNacimientoPasajero());
                  return passenger;
                })
            .toList();
    return createOrder(userId, reservationId, passengers, currency);
  }

  @Transactional
  public void savePreference(String orderCode, String preferenceId, String checkoutUrl) {
    final PaymentOrder order = getLockedOrder(orderCode);
    if (order.getStatus() != PaymentOrderStatus.PENDING) {
      throw new IllegalStateException("La orden ya no admite iniciar el pago.");
    }
    order.setPreferenceId(preferenceId);
    order.setCheckoutUrl(checkoutUrl);
    order.setUpdatedAt(LocalDateTime.now());
  }

  @Transactional
  public void markGatewayError(String orderCode) {
    orderRepository
        .findByOrderCodeWithLock(orderCode)
        .filter(order -> order.getStatus() == PaymentOrderStatus.PENDING)
        .ifPresent(
            order -> {
              order.setStatus(PaymentOrderStatus.REJECTED);
              order.setUpdatedAt(LocalDateTime.now());
            });
  }

  @Transactional
  public PaymentOrderStatus applyPayment(PaymentGateway.GatewayPayment payment) {
    final PaymentOrder order = getLockedOrder(payment.externalReference());
    if (order.getAmount().compareTo(payment.amount()) != 0
        || !order.getCurrency().equals(payment.currency())) {
      throw new IllegalArgumentException(
          "El importe o la moneda del pago no coinciden con la orden.");
    }
    if (order.getStatus() == PaymentOrderStatus.APPROVED
        || order.getStatus() == PaymentOrderStatus.REFUNDED) {
      if (!payment.id().equals(order.getExternalTransactionId())) {
        throw new IllegalStateException("La orden ya fue confirmada con otra transacción.");
      }
      return order.getStatus();
    }

    final String status = payment.status().toLowerCase();
    if (order.getStatus() == PaymentOrderStatus.EXPIRED && !"approved".equals(status)) {
      return order.getStatus();
    }
    if ("approved".equals(status)) {
      order.setExternalTransactionId(payment.id());
      if (!order.getReservaTemporal().getFechaExpiracion().isAfter(LocalDateTime.now())) {
        order.setStatus(PaymentOrderStatus.REFUND_PENDING);
        order.setUpdatedAt(LocalDateTime.now());
        return order.getStatus();
      }
      final ReservaTemporal reservation =
          reservationRepository
              .findByIdWithLock(order.getReservaTemporal().getId())
              .orElseThrow(() -> new IllegalStateException("La reserva temporal ya no existe."));
      if (!reservation.getFechaExpiracion().isAfter(LocalDateTime.now())) {
        order.setStatus(PaymentOrderStatus.REFUND_PENDING);
        order.setUpdatedAt(LocalDateTime.now());
        return order.getStatus();
      }
      finalizePurchase(order, reservation);
      return order.getStatus();
    }
    if ("rejected".equals(status) || "cancelled".equals(status) || "refunded".equals(status)) {
      order.setStatus(PaymentOrderStatus.REJECTED);
      order.setExternalTransactionId(payment.id());
      order.setUpdatedAt(LocalDateTime.now());
    }
    return order.getStatus();
  }

  @Transactional
  public void markRefunded(String paymentId) {
    orderRepository
        .findByExternalTransactionIdAndStatus(paymentId, PaymentOrderStatus.REFUND_PENDING)
        .ifPresent(
            order -> {
              order.setStatus(PaymentOrderStatus.REFUNDED);
              order.setUpdatedAt(LocalDateTime.now());
            });
  }

  @Transactional(readOnly = true)
  public PaymentOrder findOwnedOrder(String orderCode, String email) {
    return orderRepository
        .findByOrderCodeAndUsuarioEmailIgnoreCase(orderCode, email)
        .orElseThrow(() -> new IllegalArgumentException("Orden de pago no encontrada."));
  }

  @Transactional
  public void rejectPendingOrder(String orderCode, String email) {
    orderRepository
        .findOwnedByOrderCodeWithLock(orderCode, email)
        .filter(order -> order.getStatus() == PaymentOrderStatus.PENDING)
        .ifPresent(
            order -> {
              order.setStatus(PaymentOrderStatus.REJECTED);
              order.setUpdatedAt(LocalDateTime.now());
            });
  }

  @Transactional(readOnly = true)
  public List<PaymentOrder> findRefundPending() {
    return orderRepository.findByStatus(PaymentOrderStatus.REFUND_PENDING);
  }

  @Transactional(readOnly = true)
  public List<String> findExpiredPendingCheckoutSessionIds() {
    return orderRepository
        .findByStatusAndReservaTemporalFechaExpiracionBefore(
            PaymentOrderStatus.PENDING, LocalDateTime.now())
        .stream()
        .map(PaymentOrder::getPreferenceId)
        .filter(sessionId -> sessionId != null && !sessionId.isBlank())
        .toList();
  }

  @Transactional
  public void markExpiredByCheckoutSessionId(String sessionId) {
    orderRepository
        .findByPreferenceIdWithLock(sessionId)
        .filter(order -> order.getStatus() == PaymentOrderStatus.PENDING)
        .filter(
            order -> !order.getReservaTemporal().getFechaExpiracion().isAfter(LocalDateTime.now()))
        .ifPresent(
            order -> {
              order.setStatus(PaymentOrderStatus.EXPIRED);
              order.setUpdatedAt(LocalDateTime.now());
            });
  }

  private void finalizePurchase(PaymentOrder order, ReservaTemporal reservation) {
    final List<ReservaAsiento> seats =
        reservationSeatRepository.findByReservaTemporalIdWithLock(reservation.getId());
    final List<Asiento> lockedSeats =
        seatRepository.findAllByIdWithLock(
            seats.stream().map(item -> item.getAsiento().getId()).toList());
    final Map<Long, Asiento> seatsById =
        lockedSeats.stream().collect(Collectors.toMap(Asiento::getId, Function.identity()));
    if (seats.isEmpty()
        || lockedSeats.size() != seats.size()
        || seats.stream()
            .anyMatch(
                seat ->
                    seat.getEstado() != EstadoReservaAsiento.RESERVADO_TEMPORAL
                        || !seatsById.containsKey(seat.getAsiento().getId())
                        || !"RESERVADO"
                            .equals(seatsById.get(seat.getAsiento().getId()).getEstado()))) {
      order.setStatus(PaymentOrderStatus.REFUND_PENDING);
      order.setUpdatedAt(LocalDateTime.now());
      return;
    }
    for (ReservaAsiento reservationSeat : seats) {
      reservationSeat.setEstado(EstadoReservaAsiento.VENDIDO);
      seatsById.get(reservationSeat.getAsiento().getId()).setEstado("VENDIDO");
    }
    reservationSeatRepository.saveAll(seats);
    seatRepository.saveAll(lockedSeats);
    order.setStatus(PaymentOrderStatus.APPROVED);
    order.setUpdatedAt(LocalDateTime.now());

    final PurchaseEmailOutbox outbox = new PurchaseEmailOutbox();
    outbox.setPaymentOrder(order);
    outbox.setStatus(EmailOutboxStatus.PENDING);
    outbox.setAttempts(0);
    outbox.setNextAttemptAt(LocalDateTime.now());
    outbox.setCreatedAt(LocalDateTime.now());
    outboxRepository.save(outbox);
  }

  private PaymentOrder getLockedOrder(String orderCode) {
    return orderRepository
        .findByOrderCodeWithLock(orderCode)
        .orElseThrow(() -> new IllegalArgumentException("Orden de pago no encontrada."));
  }

  public record PendingOrder(
      String orderCode,
      BigDecimal amount,
      String currency,
      String payerEmail,
      LocalDateTime reservationExpiry) {}
}
