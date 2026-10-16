package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PdfGeneratorService {

  public Map<String, byte[]> generarComprobantes(
      PaymentOrder order, List<ReservaAsiento> reservationSeats) {
    if (reservationSeats.isEmpty()) {
      throw new IllegalArgumentException("La compra confirmada no tiene pasajeros.");
    }
    final Map<String, byte[]> attachments = new LinkedHashMap<>();
    for (ReservaAsiento item : reservationSeats) {
      final String seat = item.getAsiento().getNumero();
      final String filename = "Pasaje-" + seat + ".pdf";
      attachments.put(filename, generateTicket(order, item));
    }
    attachments.put(
        "Factura-" + order.getOrderCode() + ".pdf", generateInvoice(order, reservationSeats));
    return attachments;
  }

  private byte[] generateTicket(PaymentOrder order, ReservaAsiento reservationSeat) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      final Document document = new Document();
      PdfWriter.getInstance(document, output);
      document.open();
      final Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
      final Font body = FontFactory.getFont(FontFactory.HELVETICA, 12);
      final var flight = reservationSeat.getAsiento().getInstanciaVuelo();
      final var scheduledFlight = flight.getVueloProgramado();
      document.add(new Paragraph("AEROLÍNEA NACIONAL - PASAJE ELECTRÓNICO", title));
      document.add(new Paragraph("Código de reserva: " + order.getOrderCode(), body));
      document.add(new Paragraph("Vuelo: " + scheduledFlight.getCodigoVuelo(), body));
      document.add(
          new Paragraph(
              "Ruta: "
                  + scheduledFlight.getOrigen().getCiudad()
                  + " → "
                  + scheduledFlight.getDestino().getCiudad(),
              body));
      document.add(new Paragraph("Horario: " + flight.getItinerarioFormateado(), body));
      document.add(
          new Paragraph(
              "Pasajero: "
                  + reservationSeat.getNombrePasajero()
                  + " "
                  + reservationSeat.getApellidoPasajero(),
              body));
      document.add(new Paragraph("Documento: " + reservationSeat.getDocumentoPasajero(), body));
      document.add(new Paragraph("Asiento: " + reservationSeat.getAsiento().getNumero(), body));
      document.add(new Paragraph("Clase: " + reservationSeat.getAsiento().getClase(), body));
      final var qr =
          new QRCodeWriter().encode(order.getOrderCode(), BarcodeFormat.QR_CODE, 180, 180);
      try (ByteArrayOutputStream qrOutput = new ByteArrayOutputStream()) {
        MatrixToImageWriter.writeToStream(qr, "PNG", qrOutput);
        document.add(Image.getInstance(qrOutput.toByteArray()));
      }
      document.close();
      return output.toByteArray();
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo generar el PDF del pasaje.", exception);
    }
  }

  private byte[] generateInvoice(PaymentOrder order, List<ReservaAsiento> reservationSeats) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      final Document document = new Document();
      PdfWriter.getInstance(document, output);
      document.open();
      final Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
      final Font body = FontFactory.getFont(FontFactory.HELVETICA, 12);
      final var firstSeat = reservationSeats.getFirst().getAsiento();
      final var flight = firstSeat.getInstanciaVuelo();
      final var scheduledFlight = flight.getVueloProgramado();
      document.add(new Paragraph("AEROLÍNEA NACIONAL - FACTURA COMERCIAL", title));
      document.add(new Paragraph("Factura / reserva: " + order.getOrderCode(), body));
      document.add(new Paragraph("Cliente: " + order.getUsuario().getEmail(), body));
      document.add(new Paragraph("Transacción: " + order.getExternalTransactionId(), body));
      document.add(new Paragraph("Vuelo: " + scheduledFlight.getCodigoVuelo(), body));
      document.add(
          new Paragraph(
              "Ruta: "
                  + scheduledFlight.getOrigen().getCiudad()
                  + " → "
                  + scheduledFlight.getDestino().getCiudad(),
              body));
      document.add(new Paragraph("Pasajeros: " + reservationSeats.size(), body));
      document.add(
          new Paragraph("Subtotal: " + order.getSubtotal() + " " + order.getCurrency(), body));
      document.add(
          new Paragraph("Impuestos: " + order.getTaxAmount() + " " + order.getCurrency(), body));
      document.add(
          new Paragraph("Importe total (" + order.getCurrency() + "): " + order.getAmount(), body));
      for (ReservaAsiento item : reservationSeats) {
        document.add(
            new Paragraph(
                item.getNombrePasajero()
                    + " "
                    + item.getApellidoPasajero()
                    + " — asiento "
                    + item.getAsiento().getNumero(),
                body));
      }
      document.close();
      return output.toByteArray();
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo generar el PDF de la factura.", exception);
    }
  }
}
