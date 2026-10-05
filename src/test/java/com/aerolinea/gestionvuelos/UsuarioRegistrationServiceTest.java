package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aerolinea.gestionvuelos.dto.RegistroUsuarioForm;
import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.RegistrationMailService;
import com.aerolinea.gestionvuelos.service.UsuarioRegistrationService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RunWith(MockitoJUnitRunner.class)
public class UsuarioRegistrationServiceTest {

  @Mock private UsuarioRepository usuarioRepository;
  @Mock private RegistrationMailService registrationMailService;

  private UsuarioRegistrationService usuarioRegistrationService;

  @Before
  public void setUp() {
    final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    usuarioRegistrationService =
        new UsuarioRegistrationService(usuarioRepository, passwordEncoder, registrationMailService);
  }

  @Test
  public void shouldRejectRegistrationWhenEmailAlreadyExists() {
    final RegistroUsuarioForm form = buildValidForm();
    when(usuarioRepository.existsByDocumento("32123123")).thenReturn(false);
    when(usuarioRepository.existsByEmailIgnoreCase("pasajero@example.com")).thenReturn(true);

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> usuarioRegistrationService.registerPassenger(form));

    assertEquals("El email ya está registrado", error.getMessage());
  }

  @Test
  public void shouldRejectRegistrationWhenDocumentAlreadyExists() {
    final RegistroUsuarioForm form = buildValidForm();
    when(usuarioRepository.existsByDocumento("32123123")).thenReturn(true);

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> usuarioRegistrationService.registerPassenger(form));

    assertEquals("El documento ya está registrado", error.getMessage());
  }

  @Test
  public void shouldRejectRegistrationWhenPasswordPolicyIsInvalid() {
    final RegistroUsuarioForm form = buildValidForm();
    form.setPassword("sololetras");
    when(usuarioRepository.existsByDocumento("32123123")).thenReturn(false);
    when(usuarioRepository.existsByEmailIgnoreCase("pasajero@example.com")).thenReturn(false);

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> usuarioRegistrationService.registerPassenger(form));

    assertEquals("La contraseña debe tener al menos una letra y un número", error.getMessage());
  }

  @Test
  public void shouldPersistPassengerWithHashedPasswordAndInactiveByDefault() {
    final RegistroUsuarioForm form = buildValidForm();
    when(usuarioRepository.existsByDocumento("32123123")).thenReturn(false);
    when(usuarioRepository.existsByEmailIgnoreCase("pasajero@example.com")).thenReturn(false);
    when(usuarioRepository.save(any(Usuario.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Usuario.class));

    usuarioRegistrationService.registerPassenger(form);

    final ArgumentCaptor<Usuario> userCaptor = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).save(userCaptor.capture());
    final Usuario persistedUser = userCaptor.getValue();
    assertEquals(RolUsuario.PASAJERO, persistedUser.getRol());
    assertEquals("pasajero@example.com", persistedUser.getEmail());
    assertNotEquals(form.getPassword(), persistedUser.getPasswordHash());
    assertFalse(persistedUser.getActivo());
    verify(registrationMailService).sendConfirmationEmail(any(Usuario.class));
  }

  @Test
  public void shouldActivateAccountWhenTokenIsValid() {
    final Usuario user = new Usuario();
    user.setTokenActivacion("token-ok");
    user.setTokenExpiraEn(LocalDateTime.now().plusHours(2));
    user.setActivo(false);
    when(usuarioRepository.findByTokenActivacion("token-ok")).thenReturn(Optional.of(user));

    usuarioRegistrationService.activateAccount("token-ok");

    assertTrue(user.getActivo());
    assertNull(user.getTokenActivacion());
    assertNull(user.getTokenExpiraEn());
    verify(usuarioRepository).save(user);
  }

  private RegistroUsuarioForm buildValidForm() {
    final RegistroUsuarioForm form = new RegistroUsuarioForm();
    form.setNombre("Ana");
    form.setApellido("Pérez");
    form.setDocumento("32123123");
    form.setEmail("Pasajero@Example.com");
    form.setTelefono("+54 11 4444-5555");
    form.setPassword("Clave123");
    return form;
  }
}
