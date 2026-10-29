package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.dto.HistorialCompraDto;
import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.aerolinea.gestionvuelos.repository.PaymentOrderRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
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
          asientos.stream()
              .map(
                  a ->
                      new HistorialCompraDto.PasajeItemDto(
                          a.getId(),
                          a.getAsiento().getNumero(),
                          a.getNombrePasajero() + " " + a.getApellidoPasajero(),
                          a.getDocumentoPasajero()))
              .toList();

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
    PaymentOrder order =
        paymentOrderRepository
            .findOwnedByOrderCodeAndStatus(codigoOrden, emailUsuario, PaymentOrderStatus.APPROVED)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró la factura o no pertenece al usuario."));

    List<ReservaAsiento> reservas =
        reservaAsientoRepository.findByReservaTemporalId(order.getReservaTemporal().getId());
    return pdfGeneratorService.generarFacturaPdf(order, reservas);
  }

  @Transactional(readOnly = true)
  public byte[] obtenerPasajePdf(Long reservaAsientoId, String emailUsuario) {
    ReservaAsiento reserva =
        reservaAsientoRepository
            .findById(reservaAsientoId)
            .orElseThrow(
                () -> new IllegalArgumentException("No se encontró el pasaje solicitado."));

    // Verifica que la reserva pertenezca al usuario que la consulta
    if (!reserva.getReservaTemporal().getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
      throw new IllegalArgumentException("No tiene permisos para descargar este pasaje.");
    }

    PaymentOrder order =
        paymentOrderRepository
            .findOwnedByReservaTemporalIdAndStatus(
                reserva.getReservaTemporal().getId(), emailUsuario, PaymentOrderStatus.APPROVED)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró la orden de pago asociada al pasaje."));

    return pdfGeneratorService.generarPasajePdf(order, reserva);
  }
}
