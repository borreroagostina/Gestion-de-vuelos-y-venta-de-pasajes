package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.dto.RegistroUsuarioForm;
import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioRegistrationService {

  private static final Pattern PASSWORD_POLICY_PATTERN =
      Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$");
  private static final int ACTIVATION_EXPIRATION_HOURS = 24;

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final RegistrationMailService registrationMailService;

  public UsuarioRegistrationService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      RegistrationMailService registrationMailService) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.registrationMailService = registrationMailService;
  }

  @Transactional
  public void registerPassenger(RegistroUsuarioForm form) {
    final String normalizedDocument = normalizeDocument(form.getDocumento());
    final String normalizedEmail = normalizeEmail(form.getEmail());
    if (usuarioRepository.existsByDocumento(normalizedDocument)) {
      throw new IllegalArgumentException("El documento ya está registrado");
    }
    if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new IllegalArgumentException("El email ya está registrado");
    }
    if (!PASSWORD_POLICY_PATTERN.matcher(form.getPassword()).matches()) {
      throw new IllegalArgumentException("La contraseña debe tener al menos una letra y un número");
    }

    final Usuario usuario = new Usuario();
    usuario.setNombre(form.getNombre().trim());
    usuario.setApellido(form.getApellido().trim());
    usuario.setDocumento(normalizedDocument);
    usuario.setEmail(normalizedEmail);
    usuario.setTelefono(form.getTelefono().trim());
    usuario.setPasswordHash(passwordEncoder.encode(form.getPassword()));
    usuario.setRol(RolUsuario.PASAJERO);
    usuario.setActivo(false);
    usuario.setTokenActivacion(UUID.randomUUID().toString());
    usuario.setTokenExpiraEn(LocalDateTime.now().plusHours(ACTIVATION_EXPIRATION_HOURS));

    final Usuario persistedUser;
    try {
      persistedUser = usuarioRepository.save(usuario);
    } catch (DataIntegrityViolationException e) {
      throw mapConstraintError(e);
    }
    registrationMailService.sendConfirmationEmail(persistedUser);
  }

  @Transactional
  public void activateAccount(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("Token de activación inválido o vencido");
    }
    final Optional<Usuario> user = usuarioRepository.findByTokenActivacion(token);
    final Usuario usuario =
        user.orElseThrow(
            () -> new IllegalArgumentException("Token de activación inválido o vencido"));
    if (usuario.getTokenExpiraEn() == null
        || usuario.getTokenExpiraEn().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Token de activación inválido o vencido");
    }
    usuario.setActivo(true);
    usuario.setTokenActivacion(null);
    usuario.setTokenExpiraEn(null);
    usuarioRepository.save(usuario);
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeDocument(String document) {
    return document.trim();
  }

  private IllegalArgumentException mapConstraintError(DataIntegrityViolationException e) {
    final String message =
        e.getMostSpecificCause() == null ? "" : e.getMostSpecificCause().getMessage();
    if (message.contains("usuarios.documento")) {
      return new IllegalArgumentException("El documento ya está registrado");
    }
    if (message.contains("usuarios.email")) {
      return new IllegalArgumentException("El email ya está registrado");
    }
    return new IllegalArgumentException("No se pudo registrar el usuario. Intente nuevamente.", e);
  }
}
