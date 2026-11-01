package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.dto.CounterSaleForm;
import com.aerolinea.gestionvuelos.dto.FormValidationIssue;
import com.aerolinea.gestionvuelos.model.MetodoPagoMostrador;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.repository.ReservaTemporalRepository;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounterSaleService {

  private final UsuarioRepository userRepository;
  private final ReservaTemporalRepository reservationRepository;
  private final ReservaAsientoRepository reservationSeatRepository;
  private final PaymentOrderService orderService;
  private final CheckoutService checkoutService;
  private final PaymentGateway paymentGateway;
  private final PasswordEncoder passwordEncoder;

  public CounterSaleService(
      UsuarioRepository userRepository,
      ReservaTemporalRepository reservationRepository,
      ReservaAsientoRepository reservationSeatRepository,
      PaymentOrderService orderService,
      CheckoutService checkoutService,
      PaymentGateway paymentGateway,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.reservationRepository = reservationRepository;
    this.reservationSeatRepository = reservationSeatRepository;
    this.orderService = orderService;
    this.checkoutService = checkoutService;
    this.paymentGateway = paymentGateway;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Consolida una venta de mostrador: los asientos fueron reservados por el empleado, se traspasan
   * al cliente y la orden queda aprobada con el cobro presencial, sin pasar por la pasarela.
   */
  @Transactional
  public CounterSaleResult registerSale(Long employeeId, Long reservationId, CounterSaleForm form) {
    final Usuario employee =
        userRepository
            .findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado."));
    if (employee.getRol() != RolUsuario.EMPLEADO && employee.getRol() != RolUsuario.ADMIN) {
      throw new IllegalArgumentException("Solo empleados o administradores registran ventas.");
    }

    final ReservaTemporal reservation =
        reservationRepository
            .findByIdWithLock(reservationId)
            .orElseThrow(() -> new IllegalStateException("La reserva temporal ya no existe."));
    if (!reservation.getUsuario().getId().equals(employeeId)) {
      throw new IllegalArgumentException("La reserva no pertenece al empleado autenticado.");
    }
    if (!reservation.getFechaExpiracion().isAfter(LocalDateTime.now())) {
      throw new IllegalStateException(
          "La reserva temporal expiró. Seleccioná los asientos otra vez.");
    }

    final List<ReservaAsiento> seats =
        reservationSeatRepository.findByReservaTemporalIdWithLock(reservationId);
    final List<FormValidationIssue> issues =
        checkoutService.validarPasajeros(form.getPasajeros(), seats.size());
    if (!issues.isEmpty()) {
      throw new IllegalArgumentException(issues.get(0).message());
    }

    final Usuario customer = resolveCustomer(form);
    reservation.setUsuario(customer);
    seats.forEach(seat -> seat.setUsuario(customer));
    reservationRepository.save(reservation);
    reservationSeatRepository.saveAll(seats);

    final PaymentOrderService.PendingOrder order =
        orderService.createOrder(
            customer.getId(), reservationId, form.getPasajeros(), paymentGateway.getCurrency());
    orderService.confirmCounterSale(order.orderCode(), employee, form.getMetodoPago());
    return new CounterSaleResult(
        order.orderCode(),
        order.amount(),
        order.currency(),
        customer.getEmail(),
        form.getMetodoPago());
  }

  private Usuario resolveCustomer(CounterSaleForm form) {
    final String email = form.getClienteEmail().trim().toLowerCase(Locale.ROOT);
    final String document = form.getClienteDocumento().trim();
    final Usuario existing = userRepository.findByEmailIgnoreCase(email).orElse(null);
    if (existing != null) {
      if (existing.getRol() != RolUsuario.PASAJERO || !existing.getDocumento().equals(document)) {
        throw new IllegalArgumentException(
            "El email ya pertenece a otra persona o cuenta. Verificá email y documento.");
      }
      return existing;
    }
    if (userRepository.existsByDocumento(document)) {
      throw new IllegalArgumentException("El documento ya está registrado con otro email.");
    }

    final Usuario customer = new Usuario();
    customer.setNombre(form.getClienteNombre().trim());
    customer.setApellido(form.getClienteApellido().trim());
    customer.setDocumento(document);
    customer.setEmail(email);
    customer.setTelefono(form.getClienteTelefono().trim());
    customer.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
    customer.setRol(RolUsuario.PASAJERO);
    customer.setActivo(false);
    return userRepository.save(customer);
  }

  public record CounterSaleResult(
      String orderCode,
      BigDecimal amount,
      String currency,
      String customerEmail,
      MetodoPagoMostrador paymentMethod) {}
}
