package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.VueloProgramado;
import com.aerolinea.gestionvuelos.repository.ReservaAsientoRepository;
import com.aerolinea.gestionvuelos.service.EmailNotificationService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RunWith(MockitoJUnitRunner.class)
public class EmailNotificationServiceTest {

  private static final Long INSTANCE_ID = 42L;
  private static final int YEAR = 2026;
  private static final int MONTH = 11;
  private static final int DEPARTURE_DAY = 5;
  private static final int OLD_ARRIVAL_HOUR = 12;
  private static final int DEPARTURE_HOUR = 14;
  private static final int ARRIVAL_HOUR = 16;
  private static final int ARRIVAL_MINUTE = 30;

  @Mock private JavaMailSender mailSender;
  @Mock private ReservaAsientoRepository reservaAsientoRepository;

  private EmailNotificationService notificationService;
  private InstanciaVuelo instancia;

  @Before
  public void setUp() {
    notificationService =
        new EmailNotificationService(mailSender, reservaAsientoRepository, "no-reply@test.com");
    final VueloProgramado vuelo = new VueloProgramado();
    vuelo.setCodigoVuelo("AR123");
    instancia = new InstanciaVuelo();
    instancia.setId(INSTANCE_ID);
    instancia.setVueloProgramado(vuelo);
    instancia.setFechaSalida(LocalDate.of(YEAR, MONTH, DEPARTURE_DAY));
    instancia.setHoraSalida(LocalTime.of(DEPARTURE_HOUR, 0));
    instancia.setHoraLlegada(LocalTime.of(ARRIVAL_HOUR, ARRIVAL_MINUTE));
  }

  @Test
  public void sendsScheduleChangeDetailsToEachAffectedBuyer() {
    when(reservaAsientoRepository.findDistinctEmailsByInstanciaAndEstado(
            INSTANCE_ID, EstadoReservaAsiento.VENDIDO))
        .thenReturn(List.of("pasajero@test.com", "otro@test.com"));

    notificationService.notificarCambioHorario(
        instancia, LocalTime.of(10, 0), LocalTime.of(OLD_ARRIVAL_HOUR, 0));

    final ArgumentCaptor<SimpleMailMessage> messages =
        ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender, org.mockito.Mockito.times(2)).send(messages.capture());
    assertEquals("Cambio de horario del vuelo AR123", messages.getAllValues().get(0).getSubject());
    assertTrue(messages.getAllValues().get(0).getText().contains("05/11/2026"));
    assertTrue(messages.getAllValues().get(0).getText().contains("salida 10:00, llegada 12:00"));
    assertTrue(messages.getAllValues().get(0).getText().contains("salida 14:00, llegada 16:30"));
    assertEquals("pasajero@test.com", messages.getAllValues().get(0).getTo()[0]);
    assertEquals("otro@test.com", messages.getAllValues().get(1).getTo()[0]);
  }

  @Test
  public void sendsCancellationDetailsToAffectedBuyer() {
    when(reservaAsientoRepository.findDistinctEmailsByInstanciaAndEstado(
            INSTANCE_ID, EstadoReservaAsiento.VENDIDO))
        .thenReturn(List.of("pasajero@test.com"));

    notificationService.notificarCancelacionVuelo(instancia, "Tormenta");

    final ArgumentCaptor<SimpleMailMessage> message =
        ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(message.capture());
    assertEquals("Vuelo cancelado AR123", message.getValue().getSubject());
    assertTrue(message.getValue().getText().contains("05/11/2026"));
    assertTrue(message.getValue().getText().contains("ha sido cancelado"));
    assertTrue(message.getValue().getText().contains("Motivo: Tormenta"));
  }
}
