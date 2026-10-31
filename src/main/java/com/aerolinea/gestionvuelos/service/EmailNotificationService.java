package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService implements NotificationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

  private final JavaMailSender mailSender;
  private final ReservaAsientoRepository reservaAsientoRepository;
  private final String fromAddress;

  public EmailNotificationService(
      JavaMailSender mailSender,
      ReservaAsientoRepository reservaAsientoRepository,
      @Value("${app.email.from:no-reply@aerolinea.local}") String fromAddress) {
    this.mailSender = mailSender;
    this.reservaAsientoRepository = reservaAsientoRepository;
    this.fromAddress = fromAddress;
  }

  @Override
  public void notificarCancelacionVuelo(InstanciaVuelo instancia, String motivo) {
    final String subject = "Vuelo cancelado " + instancia.getVueloProgramado().getCodigoVuelo();
    final String body =
        "Te informamos que el vuelo "
            + instancia.getVueloProgramado().getCodigoVuelo()
            + " del "
            + instancia.getFechaSalida().format(DATE_FORMAT)
            + " ha sido cancelado."
            + (motivo == null || motivo.isBlank() ? "" : "\nMotivo: " + motivo)
            + "\n\nPor favor, comunícate con la aerolínea para reorganizar tu viaje.";
    enviarAfectados(instancia, subject, body);
  }

  @Override
  public void notificarCambioHorario(
      InstanciaVuelo instancia, LocalTime horaSalidaAnterior, LocalTime horaLlegadaAnterior) {
    final String codigoVuelo = instancia.getVueloProgramado().getCodigoVuelo();
    final String subject = "Cambio de horario del vuelo " + codigoVuelo;
    final String body =
        "Te informamos que cambió el horario del vuelo "
            + codigoVuelo
            + " del "
            + instancia.getFechaSalida().format(DATE_FORMAT)
            + "."
            + "\nHorario anterior: salida "
            + horaSalidaAnterior.format(TIME_FORMAT)
            + ", llegada "
            + horaLlegadaAnterior.format(TIME_FORMAT)
            + "."
            + "\nNuevo horario: salida "
            + instancia.getHoraSalida().format(TIME_FORMAT)
            + ", llegada "
            + instancia.getHoraLlegada().format(TIME_FORMAT)
            + ".";
    enviarAfectados(instancia, subject, body);
  }

  private void enviarAfectados(InstanciaVuelo instancia, String subject, String body) {
    final List<String> emails =
        reservaAsientoRepository.findDistinctEmailsByInstanciaAndEstado(
            instancia.getId(), EstadoReservaAsiento.VENDIDO);
    for (final String email : emails) {
      final SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromAddress);
      message.setTo(email);
      message.setSubject(subject);
      message.setText(body);
      try {
        mailSender.send(message);
      } catch (MailException exception) {
        LOGGER.error("No se pudo enviar la notificación del vuelo a {}", email, exception);
        throw new IllegalStateException(
            "No se pudo enviar la notificación del vuelo a " + email + ".", exception);
      }
    }
  }
}
