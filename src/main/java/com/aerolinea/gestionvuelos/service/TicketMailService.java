package com.aerolinea.gestionvuelos.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class TicketMailService {

  private final JavaMailSender mailSender;
  private final String fromAddress;

  public TicketMailService(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      @Value("${app.email.from:no-reply@aerolinea.local}") String fromAddress) {
    this.mailSender = mailSenderProvider.getIfAvailable();
    this.fromAddress = fromAddress;
  }

  public void enviarComprobantes(
      String emailDestino, String codigoReserva, Map<String, byte[]> pdfs) {
    if (mailSender == null) {
      throw new IllegalStateException("El servicio de correo no está disponible.");
    }
    try {
      final MimeMessage message = mailSender.createMimeMessage();
      final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(emailDestino);
      helper.setSubject("Tus pasajes y factura - Reserva " + codigoReserva);
      helper.setText(
          "Gracias por tu compra. Adjuntamos un pasaje electrónico por viajero "
              + "y la factura consolidada.\n"
              + "Código de reserva: "
              + codigoReserva);
      for (Map.Entry<String, byte[]> attachment : pdfs.entrySet()) {
        helper.addAttachment(attachment.getKey(), new ByteArrayResource(attachment.getValue()));
      }
      mailSender.send(message);
    } catch (MessagingException | MailException exception) {
      throw new IllegalStateException(
          "No se pudieron enviar los comprobantes por correo.", exception);
    }
  }
}
