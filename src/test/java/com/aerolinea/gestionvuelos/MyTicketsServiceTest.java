package com.aerolinea.gestionvuelos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aerolinea.gestionvuelos.dto.HistorialCompraDto;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.model.VueloProgramado;
import com.aerolinea.gestionvuelos.repository.PaymentOrderRepository;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.service.MyTicketsService;
import com.aerolinea.gestionvuelos.service.PdfGeneratorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyTicketsServiceTest {

  @Mock private PaymentOrderRepository paymentOrderRepository;
  @Mock private ReservaAsientoRepository reservaAsientoRepository;
  @Mock private PdfGeneratorService pdfGeneratorService;

  @InjectMocks private MyTicketsService myTicketsService;

  @Test
  void testObtenerHistorialComprasExitoso() {
    final String email = "pasajero@test.com";
    final PaymentOrder order = new PaymentOrder();
    order.setOrderCode("ORD-123");
    order.setAmount(new BigDecimal("1000.00"));

    final ReservaTemporal resTemp = new ReservaTemporal();
    resTemp.setId(10L);
    order.setReservaTemporal(resTemp);

    final Aeropuerto orig = new Aeropuerto();
    orig.setCiudad("Buenos Aires");
    final Aeropuerto dest = new Aeropuerto();
    dest.setCiudad("Mendoza");

    final VueloProgramado vp = new VueloProgramado();
    vp.setCodigoVuelo("AR1234");
    vp.setOrigen(orig);
    vp.setDestino(dest);

    final InstanciaVuelo iv = new InstanciaVuelo();
    iv.setVueloProgramado(vp);
    iv.setFechaSalida(LocalDate.now().plusDays(1));
    iv.setHoraSalida(LocalTime.of(10, 0));

    final Asiento asiento = new Asiento();
    asiento.setNumero("12A");
    asiento.setInstanciaVuelo(iv);

    final ReservaAsiento ra = new ReservaAsiento();
    ra.setId(100L);
    ra.setAsiento(asiento);
    ra.setNombrePasajero("Juan");
    ra.setApellidoPasajero("Pérez");
    ra.setDocumentoPasajero("12345678");

    when(paymentOrderRepository.findHistorialUsuario(eq(email), any())).thenReturn(List.of(order));
    when(reservaAsientoRepository.findByReservaTemporalId(10L)).thenReturn(List.of(ra));

    final List<HistorialCompraDto> resultado = myTicketsService.obtenerHistorialCompras(email);

    assertEquals(1, resultado.size());
    assertEquals("ORD-123", resultado.get(0).orderCode());
    assertEquals("AR1234", resultado.get(0).codigoVuelo());
    assertEquals(1, resultado.get(0).pasajes().size());
  }

  @Test
  void testObtenerPasajePdfNoOwnerThrowsException() {
    final String email = "otro@test.com";

    final Usuario usuarioOriginal = new Usuario();
    usuarioOriginal.setEmail("dueno@test.com");

    final ReservaTemporal resTemp = new ReservaTemporal();
    resTemp.setUsuario(usuarioOriginal);

    final ReservaAsiento ra = new ReservaAsiento();
    ra.setId(100L);
    ra.setReservaTemporal(resTemp);

    when(reservaAsientoRepository.findById(100L)).thenReturn(Optional.of(ra));

    assertThrows(
        IllegalArgumentException.class, () -> myTicketsService.obtenerPasajePdf(100L, email));
    verify(paymentOrderRepository, never())
        .findOwnedByReservaTemporalIdAndStatus(anyLong(), anyString(), any());
  }

  @Test
  void testObtenerFacturaPdfSoloParaOrdenAprobadaDelUsuario() {
    final String email = "pasajero@test.com";
    final PaymentOrder order = new PaymentOrder();
    order.setOrderCode("ORD-123");
    order.setStatus(PaymentOrderStatus.APPROVED);
    final ReservaTemporal reservaTemporal = new ReservaTemporal();
    reservaTemporal.setId(10L);
    order.setReservaTemporal(reservaTemporal);
    when(paymentOrderRepository.findOwnedByOrderCodeAndStatus(
            "ORD-123", email, PaymentOrderStatus.APPROVED))
        .thenReturn(Optional.of(order));
    when(reservaAsientoRepository.findByReservaTemporalId(10L)).thenReturn(List.of());
    when(pdfGeneratorService.generarFacturaPdf(order, List.of())).thenReturn(new byte[] {1, 2});

    assertArrayEquals(new byte[] {1, 2}, myTicketsService.obtenerFacturaPdf("ORD-123", email));
    verify(paymentOrderRepository)
        .findOwnedByOrderCodeAndStatus("ORD-123", email, PaymentOrderStatus.APPROVED);
  }

  @Test
  void testObtenerFacturaPdfRechazaOrdenAjenaONoAprobada() {
    when(paymentOrderRepository.findOwnedByOrderCodeAndStatus(
            "ORD-123", "otro@test.com", PaymentOrderStatus.APPROVED))
        .thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () -> myTicketsService.obtenerFacturaPdf("ORD-123", "otro@test.com"));
    verify(pdfGeneratorService, never()).generarFacturaPdf(any(), anyList());
  }

  @Test
  void testObtenerPasajePdfSoloParaReservaDelUsuarioConOrdenAprobada() {
    final String email = "pasajero@test.com";
    final Usuario usuario = new Usuario();
    usuario.setEmail(email);
    final ReservaTemporal reservaTemporal = new ReservaTemporal();
    reservaTemporal.setId(10L);
    reservaTemporal.setUsuario(usuario);
    final ReservaAsiento reserva = new ReservaAsiento();
    reserva.setId(100L);
    reserva.setReservaTemporal(reservaTemporal);
    final PaymentOrder order = new PaymentOrder();
    order.setStatus(PaymentOrderStatus.APPROVED);
    when(reservaAsientoRepository.findById(100L)).thenReturn(Optional.of(reserva));
    when(paymentOrderRepository.findOwnedByReservaTemporalIdAndStatus(
            10L, email, PaymentOrderStatus.APPROVED))
        .thenReturn(Optional.of(order));
    when(pdfGeneratorService.generarPasajePdf(order, reserva)).thenReturn(new byte[] {3});

    assertArrayEquals(new byte[] {3}, myTicketsService.obtenerPasajePdf(100L, email));
    verify(pdfGeneratorService).generarPasajePdf(order, reserva);
  }

  @Test
  void testHistorialConsultaSoloOrdenesAprobadasDelUsuario() {
    when(paymentOrderRepository.findHistorialUsuario(
            "pasajero@test.com", PaymentOrderStatus.APPROVED))
        .thenReturn(List.of());

    assertTrue(myTicketsService.obtenerHistorialCompras("pasajero@test.com").isEmpty());
    verify(paymentOrderRepository)
        .findHistorialUsuario("pasajero@test.com", PaymentOrderStatus.APPROVED);
  }
}
