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
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.PassengerCheckoutService;
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
  private static final int BIRTH_YEAR = 1990;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UsuarioRepository usuarioRepository;

  @MockitoBean private PassengerCheckoutService passengerCheckoutService;

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
