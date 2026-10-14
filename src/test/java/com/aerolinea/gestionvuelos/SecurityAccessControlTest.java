package com.aerolinea.gestionvuelos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aerolinea.gestionvuelos.config.SecurityConfig;
import com.aerolinea.gestionvuelos.controller.AdminCatalogController;
import com.aerolinea.gestionvuelos.controller.AdminController;
import com.aerolinea.gestionvuelos.controller.CounterEmployeeController;
import com.aerolinea.gestionvuelos.controller.HomeController;
import com.aerolinea.gestionvuelos.controller.LoginController;
import com.aerolinea.gestionvuelos.controller.PassengerController;
import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.CatalogService;
import com.aerolinea.gestionvuelos.service.PassengerCheckoutService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest({
  HomeController.class,
  LoginController.class,
  PassengerController.class,
  CounterEmployeeController.class,
  AdminController.class,
  AdminCatalogController.class
})
@Import(SecurityConfig.class)
class SecurityAccessControlTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockitoBean private UsuarioRepository usuarioRepository;

  @MockitoBean private CatalogService catalogService;

  @MockitoBean private PassengerCheckoutService passengerCheckoutService;

  @BeforeEach
  void setUp() {
    when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
  }

  @Test
  void shouldLoginRegisteredActivatedPassengerWithEmail() throws Exception {
    when(usuarioRepository.findByEmailIgnoreCase("pasajero@test.com"))
        .thenReturn(Optional.of(activeUser("pasajero@test.com", RolUsuario.PASAJERO)));

    mockMvc
        .perform(
            post("/login")
                .param("email", "pasajero@test.com")
                .param("password", "Clave1234")
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/pasajero"));
  }

  @Test
  void shouldShowGenericMessageForInvalidCredentials() throws Exception {
    when(usuarioRepository.findByEmailIgnoreCase("pasajero@test.com"))
        .thenReturn(Optional.of(activeUser("pasajero@test.com", RolUsuario.PASAJERO)));

    mockMvc
        .perform(
            post("/login")
                .param("email", "pasajero@test.com")
                .param("password", "incorrecta")
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));

    mockMvc
        .perform(get("/login").param("error", ""))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("No se pudo iniciar sesión con los datos ingresados.")));
  }

  @Test
  @WithMockUser(roles = "PASAJERO")
  void shouldRejectDirectAccessToAdminUrlsForPassenger() throws Exception {
    mockMvc
        .perform(get("/admin"))
        .andExpect(status().isForbidden())
        .andExpect(forwardedUrl("/acceso-denegado"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminMenuShouldNotExposePassengerOptions() throws Exception {
    mockMvc
        .perform(get("/admin"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Menú del rol administrador")))
        .andExpect(content().string(not(containsString("Comprar pasajes"))));
  }

  @Test
  @WithMockUser(roles = "PASAJERO")
  void passengerMenuShouldNotExposeFlightManagementOptions() throws Exception {
    mockMvc
        .perform(get("/pasajero"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Menú del rol pasajero")))
        .andExpect(content().string(not(containsString("Gestionar vuelos"))));
  }

  @Test
  void logoutShouldInvalidateSessionAndDeleteSessionCookie() throws Exception {
    when(usuarioRepository.findByEmailIgnoreCase("admin@test.com"))
        .thenReturn(Optional.of(activeUser("admin@test.com", RolUsuario.ADMIN)));

    final MvcResult loginResult =
        mockMvc
            .perform(
                post("/login")
                    .param("email", "admin@test.com")
                    .param("password", "Clave1234")
                    .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin"))
            .andReturn();

    mockMvc
        .perform(
            post("/logout")
                .session((MockHttpSession) loginResult.getRequest().getSession(false))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?logout"))
        .andExpect(cookie().maxAge("JSESSIONID", 0));
  }

  @Test
  void shouldShowExpiredSessionMessage() throws Exception {
    mockMvc
        .perform(get("/login").param("expired", ""))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("La sesión expiró por inactividad.")));
  }

  private Usuario activeUser(String email, RolUsuario rolUsuario) {
    final Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPasswordHash(passwordEncoder.encode("Clave1234"));
    usuario.setRol(rolUsuario);
    usuario.setActivo(true);
    usuario.setNombre("Test");
    usuario.setApellido("User");
    usuario.setDocumento("12345678");
    usuario.setTelefono("123456789");
    return usuario;
  }
}
