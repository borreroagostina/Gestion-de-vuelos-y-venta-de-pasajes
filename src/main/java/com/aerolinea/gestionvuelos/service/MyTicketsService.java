package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.dto.HistorialCompraDto;
import com.aerolinea.gestionvuelos.model.EstadoReembolso;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.aerolinea.gestionvuelos.repository.PaymentOrderRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyTicketsService {

  private final PaymentOrderRepository paymentOrderRepository;
  private final ReservaAsientoRepository reservaAsientoRepository;
  private final PdfGeneratorService pdfGeneratorService;

  public MyTicketsService(
      PaymentOrderRepository paymentOrderRepository,
      ReservaAsientoRepository reservaAsientoRepository,
      PdfGeneratorService pdfGeneratorService) {
    this.paymentOrderRepository = paymentOrderRepository;
    this.reservaAsientoRepository = reservaAsientoRepository;
    this.pdfGeneratorService = pdfGeneratorService;
  }

  @Transactional(readOnly = true)
  public List<HistorialCompraDto> obtenerHistorialCompras(String email) {
    final List<PaymentOrder> ordenes =
        paymentOrderRepository.findHistorialUsuario(email, PaymentOrderStatus.APPROVED);
    final List<HistorialCompraDto> historial = new ArrayList<>();

    for (PaymentOrder orden : ordenes) {
      final List<ReservaAsiento> asientos =
          reservaAsientoRepository.findByReservaTemporalId(orden.getReservaTemporal().getId());
      if (asientos.isEmpty()) {
        continue;
      }

      final var primerAsiento = asientos.get(0).getAsiento();
      final var vuelo = primerAsiento.getInstanciaVuelo();

      final List<HistorialCompraDto.PasajeItemDto> pasajesDto =
          asientos.stream().map(a -> toPasajeDto(a, orden, asientos.size(), vuelo)).toList();

      historial.add(
          new HistorialCompraDto(
              orden.getOrderCode(),
              vuelo.getVueloProgramado().getCodigoVuelo(),
              vuelo.getVueloProgramado().getOrigen().getCiudad(),
              vuelo.getVueloProgramado().getDestino().getCiudad(),
              vuelo.getFechaSalida().atTime(vuelo.getHoraSalida()),
              pasajesDto,
              orden.getAmount(),
              orden.getUpdatedAt()));
    }

    return historial;
  }

  @Transactional(readOnly = true)
  public byte[] obtenerFacturaPdf(String codigoOrden, String emailUsuario) {
    final PaymentOrder order =
        paymentOrderRepository
            .findOwnedByOrderCodeAndStatus(codigoOrden, emailUsuario, PaymentOrderStatus.APPROVED)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró la factura o no pertenece al usuario."));

    final List<ReservaAsiento> reservas =
        reservaAsientoRepository.findByReservaTemporalId(order.getReservaTemporal().getId());
    return pdfGeneratorService.generarFacturaPdf(order, reservas);
  }

  @Transactional(readOnly = true)
  public byte[] obtenerPasajePdf(Long reservaAsientoId, String emailUsuario) {
    final ReservaAsiento reserva =
        reservaAsientoRepository
            .findById(reservaAsientoId)
            .orElseThrow(
                () -> new IllegalArgumentException("No se encontró el pasaje solicitado."));

    if (reserva.getEstado() == EstadoReservaAsiento.CANCELADO) {
      throw new IllegalStateException("El pasaje fue cancelado y ya no puede descargarse.");
    }

    // Verifica que la reserva pertenezca al usuario que la consulta
    if (!reserva.getReservaTemporal().getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
      throw new IllegalArgumentException("No tiene permisos para descargar este pasaje.");
    }

    final PaymentOrder order =
        paymentOrderRepository
            .findOwnedByReservaTemporalIdAndStatus(
                reserva.getReservaTemporal().getId(), emailUsuario, PaymentOrderStatus.APPROVED)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró la orden de pago asociada al pasaje."));

    return pdfGeneratorService.generarPasajePdf(order, reserva);
  }

  private HistorialCompraDto.PasajeItemDto toPasajeDto(
      ReservaAsiento reserva, PaymentOrder orden, int cantidadPasajes, InstanciaVuelo vuelo) {
    final boolean cancelado = reserva.getEstado() == EstadoReservaAsiento.CANCELADO;
    final LocalDateTime salida = vuelo.getFechaSalida().atTime(vuelo.getHoraSalida());
    final LocalDateTime ahora = LocalDateTime.now();
    final boolean pagoOnline =
        orden.getPaymentMethod() == null
            && orden.getExternalTransactionId() != null
            && !orden.getExternalTransactionId().isBlank();
    final boolean puedeCancelar = !cancelado && pagoOnline && salida.isAfter(ahora);

    int porcentaje = 0;
    BigDecimal reembolso = reserva.getMontoReembolso();
    if (puedeCancelar) {
      porcentaje =
          vuelo.isCancelada()
              ? RefundPolicy.FULL_PERCENT
              : RefundPolicy.percentage(
                  reserva.getAsiento().getClase(), Duration.between(ahora, salida));
      final BigDecimal precioPasaje =
          orden.getAmount().divide(BigDecimal.valueOf(cantidadPasajes), 2, RoundingMode.HALF_UP);
      reembolso = RefundPolicy.refundAmount(precioPasaje, porcentaje);
    }
    return new HistorialCompraDto.PasajeItemDto(
        reserva.getId(),
        reserva.getAsiento().getNumero(),
        reserva.getNombrePasajero() + " " + reserva.getApellidoPasajero(),
        reserva.getDocumentoPasajero(),
        cancelado,
        puedeCancelar,
        porcentaje,
        reembolso,
        reserva.getEstadoReembolso() == EstadoReembolso.PENDIENTE);
  }
}
