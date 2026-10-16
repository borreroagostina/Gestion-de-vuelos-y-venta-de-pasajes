package com.aerolinea.gestionvuelos;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.aerolinea.gestionvuelos.config.SecurityConfig;
import com.aerolinea.gestionvuelos.controller.PassengerController;
import com.aerolinea.gestionvuelos.dto.DatosPasajeroForm;
import com.aerolinea.gestionvuelos.dto.DatosPasajerosForm;
import com.aerolinea.gestionvuelos.dto.ResumenCompraView;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.CheckoutPaymentService;
import com.aerolinea.gestionvuelos.service.PassengerCheckoutService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PassengerController.class)
@Import(SecurityConfig.class)
class PassengerCheckoutControllerTest {

  private static final long FIRST_RESERVATION_ID = 10L;
  private static final long SECOND_RESERVATION_ID = 11L;
  private static final long PAYMENT_RESERVATION_ID = 42L;
  private static final long PAYMENT_USER_ID = 7L;
  private static final int BIRTH_YEAR = 1990;
  private static final LocalDate PAYMENT_FLIGHT_DATE = LocalDate.of(2026, 11, 1);

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UsuarioRepository usuarioRepository;

  @MockitoBean private PassengerCheckoutService passengerCheckoutService;

  @MockitoBean private CheckoutPaymentService checkoutPaymentService;

  @BeforeEach
  void setUp() {
    when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    when(passengerCheckoutService.obtenerClaseReserva(anyString(), anyString()))
        .thenReturn("Economy");
    when(passengerCheckoutService.obtenerAsientos(anyString(), anyString()))
        .thenReturn(List.of("07A", "07B"));
  }

  @Test
  @org.springframework.security.test.context.support.WithMockUser(roles = "PASAJERO")
  void shouldRenderOnePassengerBlockPerReservedSeat() throws Exception {
    when(passengerCheckoutService.crearFormulario(anyString(), anyString()))
        .thenReturn(formularioConDosPasajeros());

    mockMvc
        .perform(get("/pasajero/checkout"))
        .andExpect(status().isOk())
        .andExpect(view().name("pasajero-checkout"))
        .andExpect(content().string(containsString("Pasajero 1")))
        .andExpect(content().string(containsString("Pasajero 2")))
        .andExpect(content().string(containsString("Asiento 07A")))
        .andExpect(content().string(containsString("Asiento 07B")));
  }

  @Test
  @org.springframework.security.test.context.support.WithMockUser(roles = "PASAJERO")
  void shouldKeepUserOnFormWhenPassengerDataIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/pasajero/checkout")
                .with(csrf())
                .param("pasajeros[0].reservaAsientoId", "10")
                .param("pasajeros[0].numeroAsiento", "07A")
                .param("pasajeros[0].clase", "Economy")
                .param("pasajeros[0].nombre", " ")
                .param("pasajeros[0].apellido", "Pérez")
                .param("pasajeros[0].documento", "ABC12345")
                .param("pasajeros[0].fechaNacimiento", LocalDate.of(BIRTH_YEAR, 1, 10).toString()))
        .andExpect(status().isOk())
        .andExpect(view().name("pasajero-checkout"))
        .andExpect(model().attributeHasFieldErrors("datosPasajerosForm", "pasajeros[0].nombre"))
        .andExpect(content().string(containsString("El nombre es obligatorio")));

    verify(passengerCheckoutService, never()).guardarPasajeros(anyString(), anyString(), any());
  }

  @Test
  @org.springframework.security.test.context.support.WithMockUser(
      username = "passenger@test.com",
      roles = "PASAJERO")
  void shouldShowContinueToPaymentButtonOnPassengerSummary() throws Exception {
    when(passengerCheckoutService.obtenerResumen(anyString(), anyString()))
        .thenReturn(
            new ResumenCompraView(
                PAYMENT_RESERVATION_ID,
                "AR123",
                "EZE",
                "COR",
                PAYMENT_FLIGHT_DATE,
                "10:00 → 12:00",
                "Economy",
                List.of("07A"),
                List.of(),
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("210.00"),
                new BigDecimal("1210.00")));

    mockMvc
        .perform(get("/pasajero/resumen"))
        .andExpect(status().isOk())
        .andExpect(view().name("pasajero-resumen"))
        .andExpect(content().string(containsString("/pasajero/resumen/pagar")))
        .andExpect(content().string(containsString("Continuar al pago seguro")));
  }

  @Test
  @org.springframework.security.test.context.support.WithMockUser(
      username = "passenger@test.com",
      roles = "PASAJERO")
  void shouldRedirectPassengerToHostedPaymentAfterSubmittingSummary() throws Exception {
    final Usuario user = new Usuario();
    user.setId(PAYMENT_USER_ID);
    user.setEmail("passenger@test.com");
    when(usuarioRepository.findByEmailIgnoreCase("passenger@test.com"))
        .thenReturn(Optional.of(user));
    when(passengerCheckoutService.obtenerReservaActivaId(anyString(), anyString()))
        .thenReturn(PAYMENT_RESERVATION_ID);
    when(checkoutPaymentService.startPaymentFromSavedPassengerData(
            PAYMENT_USER_ID, PAYMENT_RESERVATION_ID))
        .thenReturn("https://payments.example/checkout");

    mockMvc
        .perform(post("/pasajero/resumen/pagar").with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:https://payments.example/checkout"));
  }

  private DatosPasajerosForm formularioConDosPasajeros() {
    final DatosPasajerosForm form = new DatosPasajerosForm();
    form.setPasajeros(
        List.of(
            crearPasajero(FIRST_RESERVATION_ID, "07A"),
            crearPasajero(SECOND_RESERVATION_ID, "07B")));
    return form;
  }

  private DatosPasajeroForm crearPasajero(Long reservaAsientoId, String numeroAsiento) {
    final DatosPasajeroForm pasajero = new DatosPasajeroForm();
    pasajero.setReservaAsientoId(reservaAsientoId);
    pasajero.setNumeroAsiento(numeroAsiento);
    pasajero.setClase("Economy");
    return pasajero;
  }
}
