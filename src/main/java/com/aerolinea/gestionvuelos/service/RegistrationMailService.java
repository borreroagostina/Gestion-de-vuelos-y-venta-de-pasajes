package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class RegistrationMailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationMailService.class);

  private final JavaMailSender mailSender;
  private final String baseUrl;
  private final String fromAddress;
  private final String smtpUsername;
  private final String smtpPassword;

  public RegistrationMailService(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      @Value("${app.base-url:http://localhost:8080}") String baseUrl,
      @Value("${app.email.from:no-reply@aerolinea.local}") String fromAddress,
      @Value("${spring.mail.username:}") String smtpUsername,
      @Value("${spring.mail.password:}") String smtpPassword) {
    this.mailSender = mailSenderProvider.getIfAvailable();
    this.baseUrl = normalizeBaseUrl(baseUrl);
    this.fromAddress = fromAddress.trim();
    this.smtpUsername = smtpUsername.trim();
    this.smtpPassword = smtpPassword.replaceAll("\\s+", "");
  }

  public void sendConfirmationEmail(Usuario usuario) {
    final String activationLink =
        resolveBaseUrl() + "/registro/confirmar?token=" + usuario.getTokenActivacion();
    final SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(usuario.getEmail());
    message.setSubject("Confirma tu cuenta de pasajero");
    message.setText(
        "Hola "
            + usuario.getNombre()
            + ",\n\n"
            + "Gracias por registrarte. Para activar tu cuenta hacé clic en el siguiente enlace:\n"
            + activationLink
            + "\n\n"
            + "Si no solicitaste este registro, podés ignorar este mensaje.");
    if (mailSender == null || smtpUsername.isBlank() || smtpPassword.isBlank()) {
      throw new IllegalStateException(
          "No se pudo enviar el email de confirmación: falta configurar MAIL_USERNAME y "
              + "MAIL_PASSWORD");
    }
    try {
      mailSender.send(message);
    } catch (MailException e) {
      LOGGER.error("Fallo enviando email de confirmación a {}", usuario.getEmail(), e);
      throw new IllegalStateException(
          "No se pudo enviar el email de confirmación. Intente nuevamente.", e);
    }
  }

  private String resolveBaseUrl() {
    try {
      return normalizeBaseUrl(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString());
    } catch (IllegalStateException e) {
      return baseUrl;
    }
  }

  private String normalizeBaseUrl(String rawBaseUrl) {
    final String trimmed = rawBaseUrl == null ? "" : rawBaseUrl.trim();
    if (trimmed.endsWith("/")) {
      return trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }
}
