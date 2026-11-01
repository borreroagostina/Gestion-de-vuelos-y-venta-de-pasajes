package com.aerolinea.gestionvuelos;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.aerolinea.gestionvuelos.config.SecurityConfig;
import com.aerolinea.gestionvuelos.controller.MyTicketsController;
import com.aerolinea.gestionvuelos.dto.HistorialCompraDto;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.MyTicketsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MyTicketsController.class)
@Import(SecurityConfig.class)
class MyTicketsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UsuarioRepository usuarioRepository;

  @MockitoBean private MyTicketsService myTicketsService;

  @Test
  @WithMockUser(username = "pasajero@test.com", roles = "PASAJERO")
  void shouldShowPassengerPurchaseHistoryForAuthenticatedUser() throws Exception {
    when(myTicketsService.obtenerHistorialCompras("pasajero@test.com")).thenReturn(List.of());

    mockMvc
        .perform(get("/pasajero/mis-pasajes"))
        .andExpect(status().isOk())
        .andExpect(view().name("pasajero-mis-pasajes"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Mis Pasajes")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Aún no tenés pasajes")));

    verify(myTicketsService).obtenerHistorialCompras("pasajero@test.com");
  }

  @Test
  @WithMockUser(username = "pasajero@test.com", roles = "PASAJERO")
  void shouldRenderTicketAndInvoiceLinksForEachPurchase() throws Exception {
    final HistorialCompraDto purchase =
        new HistorialCompraDto(
            "ORD-123",
            "AR123",
            "Buenos Aires",
            "Mendoza",
            LocalDateTime.of(2026, 11, 1, 10, 0),
            List.of(new HistorialCompraDto.PasajeItemDto(100L, "07A", "Ana Pérez", "12345678")),
            new BigDecimal("1210.00"),
            LocalDateTime.of(2026, 10, 20, 12, 30));
    when(myTicketsService.obtenerHistorialCompras("pasajero@test.com"))
        .thenReturn(List.of(purchase));

    mockMvc
        .perform(get("/pasajero/mis-pasajes"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Orden #ORD-123")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString("/pasajero/mis-pasajes/pasaje/100/pdf")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "/pasajero/mis-pasajes/orden/ORD-123/factura")));
  }

  @Test
  @WithMockUser(username = "pasajero@test.com", roles = "PASAJERO")
  void shouldDownloadOwnedInvoiceAsPdf() throws Exception {
    when(myTicketsService.obtenerFacturaPdf("ORD-123", "pasajero@test.com"))
        .thenReturn(new byte[] {1, 2, 3});

    mockMvc
        .perform(get("/pasajero/mis-pasajes/orden/ORD-123/factura"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=Factura_ORD-123.pdf"))
        .andExpect(content().bytes(new byte[] {1, 2, 3}));
  }

  @Test
  @WithMockUser(username = "pasajero@test.com", roles = "PASAJERO")
  void shouldDownloadTicketAsPdf() throws Exception {
    when(myTicketsService.obtenerPasajePdf(100L, "pasajero@test.com"))
        .thenReturn(new byte[] {4, 5, 6});

    mockMvc
        .perform(get("/pasajero/mis-pasajes/pasaje/100/pdf"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=Pasaje_100.pdf"))
        .andExpect(content().bytes(new byte[] {4, 5, 6}));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldRestrictHistoryToPassengers() throws Exception {
    mockMvc.perform(get("/pasajero/mis-pasajes")).andExpect(status().isForbidden());
  }
}
