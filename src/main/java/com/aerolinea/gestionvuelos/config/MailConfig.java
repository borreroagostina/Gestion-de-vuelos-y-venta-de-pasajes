package com.aerolinea.gestionvuelos.config;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

  private static final String SMTP_PROTOCOL = "smtp";

  @Bean
  public JavaMailSender javaMailSender(
      @Value("${spring.mail.host:smtp.gmail.com}") String host,
      @Value("${spring.mail.port:587}") int port,
      @Value("${spring.mail.username:}") String username,
      @Value("${spring.mail.password:}") String password,
      @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean auth,
      @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean startTlsEnable,
      @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
          boolean startTlsRequired) {
    final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(host.trim());
    mailSender.setPort(port);
    mailSender.setUsername(username.trim());
    mailSender.setPassword(password.replaceAll("\\s+", ""));

    final Properties properties = mailSender.getJavaMailProperties();
    properties.put("mail.transport.protocol", SMTP_PROTOCOL);
    properties.put("mail.smtp.auth", auth);
    properties.put("mail.smtp.starttls.enable", startTlsEnable);
    properties.put("mail.smtp.starttls.required", startTlsRequired);
    return mailSender;
  }
}
