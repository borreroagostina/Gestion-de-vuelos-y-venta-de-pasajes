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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aerolinea.gestionvuelos.config.SecurityConfig;
import com.aerolinea.gestionvuelos.controller.AdminCatalogController;
import com.aerolinea.gestionvuelos.controller.AdminController;
import com.aerolinea.gestionvuelos.controller.AdminOccupancyReportController;
import com.aerolinea.gestionvuelos.controller.CounterEmployeeController;
import com.aerolinea.gestionvuelos.controller.HomeController;
import com.aerolinea.gestionvuelos.controller.LoginController;
import com.aerolinea.gestionvuelos.controller.PassengerController;
import com.aerolinea.gestionvuelos.dto.OcupacionVueloDto;
import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
<<<<<<< Updated upstream
import com.aerolinea.gestionvuelos.service.*;

=======
import com.aerolinea.gestionvuelos.service.CatalogService;
import com.aerolinea.gestionvuelos.service.CheckoutPaymentService;
import com.aerolinea.gestionvuelos.service.OcupacionReportService;
import com.aerolinea.gestionvuelos.service.PassengerCheckoutService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
>>>>>>> Stashed changes
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
  AdminCatalogController.class,
  AdminOccupancyReportController.class
})
@Import(SecurityConfig.class)
class SecurityAccessControlTest {

  private static final long REPORTE_VUELO_ID = 7L;
  private static final long REPORTE_ASIENTOS_VENDIDOS = 25L;
  private static final LocalDate REPORTE_FECHA_DESDE = LocalDate.parse("2026-10-01");
  private static final LocalDate REPORTE_FECHA_HASTA = LocalDate.parse("2026-10-31");
  private static final LocalDate REPORTE_FECHA_SALIDA = LocalDate.parse("2026-10-15");

  @Autowired private MockMvc mockMvc;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockitoBean private UsuarioRepository usuarioRepository;

  @MockitoBean private CatalogService catalogService;

  @MockitoBean private PassengerCheckoutService passengerCheckoutService;

  @MockitoBean private CheckoutPaymentService checkoutPaymentService;

<<<<<<< Updated upstream
  @MockitoBean private FlightService flightService;

  @MockitoBean private MyTicketsService myTicketsService;
=======
  @MockitoBean private OcupacionReportService ocupacionReportService;
>>>>>>> Stashed changes

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
        .perform(get("/admin/reportes/ocupacion"))
        .andExpect(status().isForbidden())
        .andExpect(forwardedUrl("/acceso-denegado"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanViewOccupancyReport() throws Exception {
    when(ocupacionReportService.listarVuelos()).thenReturn(List.of());
    when(ocupacionReportService.generarReporte(null, null, null, null)).thenReturn(List.of());

    mockMvc
        .perform(get("/admin/reportes/ocupacion"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Reporte de ocupación")))
        .andExpect(
            content().string(containsString("No hay vuelos para los filtros seleccionados.")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanDownloadOccupancyReportAsCsv() throws Exception {
    when(ocupacionReportService.generarReporte(
            REPORTE_VUELO_ID, "ECONOMY", REPORTE_FECHA_DESDE, REPORTE_FECHA_HASTA))
        .thenReturn(
            List.of(
                new OcupacionVueloDto(
                    "AR101",
                    REPORTE_FECHA_SALIDA,
                    "EZE",
                    "COR",
                    "ECONOMY",
                    REPORTE_ASIENTOS_VENDIDOS,
                    100,
                    new BigDecimal("25.00"))));

    mockMvc
        .perform(
            get("/admin/reportes/ocupacion/exportar")
                .param("vueloId", "7")
                .param("clase", "ECONOMY")
                .param("fechaDesde", "2026-10-01")
                .param("fechaHasta", "2026-10-31"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString("reporte-ocupacion.csv")))
        .andExpect(content().string(containsString("\"AR101\",\"2026-10-15\",\"EZE\",\"COR\"")))
        .andExpect(content().string(containsString("25,100,25.00")));
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
