package com.aerolinea.gestionvuelos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.VueloProgramado;
import com.aerolinea.gestionvuelos.repository.AeronaveRepository;
import com.aerolinea.gestionvuelos.repository.AeropuertoRepository;
import com.aerolinea.gestionvuelos.repository.InstanciaVueloRepository;
import com.aerolinea.gestionvuelos.repository.VueloProgramadoRepository;
import com.aerolinea.gestionvuelos.service.FlightService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

@RunWith(MockitoJUnitRunner.class)
public class FlightServiceValidationTest {

    private static final Long ID_EZE = 1L;
    private static final Long ID_MIA = 2L;
    private static final int SEATS_ECO = 150;
    private static final int SEATS_PRI = 20;

    @Mock private VueloProgramadoRepository vueloRepo;
    @Mock private InstanciaVueloRepository instanciaRepo;
    @Mock private AeropuertoRepository aeroRepo;
    @Mock private AeronaveRepository aeronaveRepo;
    @Mock private JdbcTemplate jdbcTemplate;

    @Captor private ArgumentCaptor<List<InstanciaVuelo>> instanciasCaptor;

    private FlightService flightService;
    private Aeropuerto aeroOrigen;
    private Aeropuerto aeroDestino;
    private Aeronave aeronave;

    @Before
    public void setUp() {
        flightService =
                new FlightService(vueloRepo, instanciaRepo, aeroRepo, aeronaveRepo, jdbcTemplate);

        aeroOrigen = new Aeropuerto("EZE", "Ezeiza", "Buenos Aires", "Argentina");
        aeroOrigen.setId(ID_EZE);

        aeroDestino = new Aeropuerto("MIA", "Miami Int", "Miami", "USA");
        aeroDestino.setId(ID_MIA);

        aeronave = new Aeronave("Boeing 737", "Boeing", "B737", SEATS_ECO, SEATS_PRI);
        aeronave.setId(10L);
    }

    @Test
    public void shouldRejectFlightWhenOriginAndDestinationAreEqual() {
        final VueloProgramado vuelo = new VueloProgramado();
        vuelo.setCodigoVuelo("AR100");
        vuelo.setOrigen(aeroOrigen);
        vuelo.setDestino(aeroOrigen);
        vuelo.setFechaInicioVenta(LocalDate.now());
        vuelo.setFechaFinVenta(LocalDate.now().plusMonths(1));
        vuelo.setDiasSemana("MONDAY");
        vuelo.setPrecioEconomy(BigDecimal.valueOf(100));
        vuelo.setPrecioPrimera(BigDecimal.valueOf(200));

        final IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> flightService.crearVuelo(vuelo));
        assertEquals("El aeropuerto de origen no puede coincidir con el destino", ex.getMessage());
    }

    @Test
    public void shouldRejectFlightWhenEndDateIsBeforeStartDate() {
        final VueloProgramado vuelo = new VueloProgramado();
        vuelo.setCodigoVuelo("AR101");
        vuelo.setOrigen(aeroOrigen);
        vuelo.setDestino(aeroDestino);
        vuelo.setFechaInicioVenta(LocalDate.now().plusDays(10));
        vuelo.setFechaFinVenta(LocalDate.now().plusDays(2));
        vuelo.setDiasSemana("MONDAY");
        vuelo.setPrecioEconomy(BigDecimal.valueOf(100));
        vuelo.setPrecioPrimera(BigDecimal.valueOf(200));

        final IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> flightService.crearVuelo(vuelo));
        assertEquals("La fecha de fin no puede ser anterior a la de inicio", ex.getMessage());
    }

    @Test
    public void shouldRejectFlightWithoutDaysOfWeek() {
        final VueloProgramado vuelo = new VueloProgramado();
        vuelo.setCodigoVuelo("AR102");
        vuelo.setOrigen(aeroOrigen);
        vuelo.setDestino(aeroDestino);
        vuelo.setFechaInicioVenta(LocalDate.now());
        vuelo.setFechaFinVenta(LocalDate.now().plusDays(5));
        vuelo.setDiasSemana("   ");
        vuelo.setPrecioEconomy(BigDecimal.valueOf(100));
        vuelo.setPrecioPrimera(BigDecimal.valueOf(200));

        final IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> flightService.crearVuelo(vuelo));
        assertEquals("Debe seleccionar al menos un dia de la semana", ex.getMessage());
    }

    @Test
    public void shouldRejectFlightWithNegativeOrZeroPrice() {
        final VueloProgramado vuelo = new VueloProgramado();
        vuelo.setCodigoVuelo("AR103");
        vuelo.setOrigen(aeroOrigen);
        vuelo.setDestino(aeroDestino);
        vuelo.setFechaInicioVenta(LocalDate.now());
        vuelo.setFechaFinVenta(LocalDate.now().plusDays(5));
        vuelo.setDiasSemana("MONDAY");
        vuelo.setPrecioEconomy(BigDecimal.ZERO);
        vuelo.setPrecioPrimera(BigDecimal.valueOf(200));

        final IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> flightService.crearVuelo(vuelo));
        assertEquals("El precio Economy debe ser positivo", ex.getMessage());
    }

    @Test
    public void shouldGenerateOnlySelectedDaysWithCorrectCapacityAndCrossMidnight() {
        // Rango: Lunes 2026-10-05 a Domingo 2026-10-11
        final LocalDate inicio = LocalDate.of(2026, 10, 5);
        final LocalDate fin = LocalDate.of(2026, 10, 11);

        final VueloProgramado vuelo = new VueloProgramado();
        vuelo.setCodigoVuelo("AR200");
        vuelo.setOrigen(aeroOrigen);
        vuelo.setDestino(aeroDestino);
        vuelo.setAeronave(aeronave);
        vuelo.setFechaInicioVenta(inicio);
        vuelo.setFechaFinVenta(fin);
        vuelo.setDiasSemana("MONDAY,WEDNESDAY");
        vuelo.setPrecioEconomy(BigDecimal.valueOf(500));
        vuelo.setPrecioPrimera(BigDecimal.valueOf(900));
        // Salida 23:00 y llegada 04:00 (cruza medianoche)
        vuelo.setHoraSalida(LocalTime.of(23, 0));
        vuelo.setHoraLlegada(LocalTime.of(4, 0));

        when(vueloRepo.save(vuelo)).thenReturn(vuelo);

        flightService.crearVuelo(vuelo);

        verify(instanciaRepo).saveAll(instanciasCaptor.capture());
        final List<InstanciaVuelo> instGeneradas = instanciasCaptor.getValue();

        // Solo debe haber 2 instancias (Lunes 5 y Miércoles 7)
        assertEquals(2, instGeneradas.size());

        for (final InstanciaVuelo inst : instGeneradas) {
            // Capacidad de la aeronave asignada
            assertEquals(Integer.valueOf(SEATS_ECO), inst.getAsientosDispEconomy());
            assertEquals(Integer.valueOf(SEATS_PRI), inst.getAsientosDispPrimera());

            // Llega al dia siguiente
            assertEquals(inst.getFechaSalida().plusDays(1), inst.getFechaLlegada());
            assertTrue(inst.cruzaMedianoche());
        }
    }

    @Test
    public void shouldRejectFlightModificationWhenNewCapacityIsLowerThanSoldSeats() {
        final LocalDate hoy = LocalDate.now();
        final VueloProgramado vueloExistente = new VueloProgramado();
        vueloExistente.setId(100L);
        vueloExistente.setCodigoVuelo("AR200");
        vueloExistente.setOrigen(aeroOrigen);
        vueloExistente.setDestino(aeroDestino);
        vueloExistente.setAeronave(aeronave); // Capacidad: 150 Eco, 20 Pri
        vueloExistente.setFechaInicioVenta(hoy);
        vueloExistente.setFechaFinVenta(hoy.plusDays(10));
        vueloExistente.setDiasSemana("MONDAY");
        vueloExistente.setPrecioEconomy(BigDecimal.valueOf(100));
        vueloExistente.setPrecioPrimera(BigDecimal.valueOf(200));

        final InstanciaVuelo instFutura = new InstanciaVuelo();
        instFutura.setId(500L);
        instFutura.setVueloProgramado(vueloExistente);
        instFutura.setFechaSalida(hoy.plusDays(2));

        when(vueloRepo.findById(100L)).thenReturn(java.util.Optional.of(vueloExistente));
        when(instanciaRepo.findByVueloProgramadoIdAndFechaSalidaGreaterThanEqual(100L, hoy))
                .thenReturn(List.of(instFutura));

        // Simulamos que ya se vendieron 50 pasajes Economy
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ventas WHERE vuelo_id = ? AND UPPER(clase) = ?",
                Integer.class,
                500L,
                "ECONOMY"))
                .thenReturn(50);

        // Intentamos cambiar a un avión con solo 30 asientos Economy (menor a los 50 vendidos)
        final Aeronave avionChico = new Aeronave("Avion Chico", "Fabricante", "CHICO", 30, 10);
        avionChico.setId(20L);

        final VueloProgramado vueloModificado = new VueloProgramado();
        vueloModificado.setId(100L);
        vueloModificado.setCodigoVuelo("AR200");
        vueloModificado.setOrigen(aeroOrigen);
        vueloModificado.setDestino(aeroDestino);
        vueloModificado.setAeronave(avionChico);
        vueloModificado.setFechaInicioVenta(hoy);
        vueloModificado.setFechaFinVenta(hoy.plusDays(10));
        vueloModificado.setDiasSemana("MONDAY");
        vueloModificado.setPrecioEconomy(BigDecimal.valueOf(100));
        vueloModificado.setPrecioPrimera(BigDecimal.valueOf(200));

        final IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class, () -> flightService.modificarVuelo(vueloModificado));
        assertTrue(ex.getMessage().contains("No se puede reducir la capacidad Economy"));
    }

    @Test
    public void shouldSuccessfullyModifyFlightAndPreserveDatesIntegrity() {
        final LocalDate hoy = LocalDate.now();
        final VueloProgramado vueloExistente = new VueloProgramado();
        vueloExistente.setId(101L);
        vueloExistente.setCodigoVuelo("AR201");
        vueloExistente.setOrigen(aeroOrigen);
        vueloExistente.setDestino(aeroDestino);
        vueloExistente.setAeronave(aeronave);
        vueloExistente.setFechaInicioVenta(hoy);
        vueloExistente.setFechaFinVenta(hoy.plusDays(5));
        vueloExistente.setDiasSemana("MONDAY");
        vueloExistente.setPrecioEconomy(BigDecimal.valueOf(100));
        vueloExistente.setPrecioPrimera(BigDecimal.valueOf(200));
        vueloExistente.setHoraSalida(LocalTime.of(10, 0));
        vueloExistente.setHoraLlegada(LocalTime.of(12, 0));

        when(vueloRepo.findById(101L)).thenReturn(java.util.Optional.of(vueloExistente));
        when(vueloRepo.save(vueloExistente)).thenReturn(vueloExistente);
        when(instanciaRepo.findByVueloProgramadoIdAndFechaSalidaGreaterThanEqual(101L, hoy))
                .thenReturn(List.of());

        // Modificamos precios y horarios
        vueloExistente.setPrecioEconomy(BigDecimal.valueOf(150));
        vueloExistente.setHoraSalida(LocalTime.of(14, 0));

        final VueloProgramado res = flightService.modificarVuelo(vueloExistente);
        assertEquals(BigDecimal.valueOf(150), res.getPrecioEconomy());
        assertEquals(LocalTime.of(14, 0), res.getHoraSalida());
    }

    @Test
    public void shouldReturnEmptyPageWhenNoFlightsMatchSearchCriteria() {
        final LocalDate maniana = LocalDate.now().plusDays(1);
        final org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);

        when(instanciaRepo.buscarVuelosPublicos(
                ID_EZE, ID_MIA, maniana, maniana, "HORA_ASC", pageable))
                .thenReturn(org.springframework.data.domain.Page.empty());

        final org.springframework.data.domain.Page<InstanciaVuelo> resultado =
                flightService.buscarVuelosPublicos(
                        ID_EZE, ID_MIA, maniana, null, "HORA_ASC", pageable);

        assertTrue(resultado.isEmpty());
    }

    @Test
    public void shouldSearchAcrossWideDateRangeSuccessfully() {
        final LocalDate inicio = LocalDate.now().plusDays(1);
        final LocalDate fin = LocalDate.now().plusMonths(3);
        final org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);

        final InstanciaVuelo inst = new InstanciaVuelo();
        inst.setFechaSalida(inicio.plusDays(15));

        final org.springframework.data.domain.Page<InstanciaVuelo> paginaConResultados =
                new org.springframework.data.domain.PageImpl<>(List.of(inst));

        when(instanciaRepo.buscarVuelosPublicos(
                ID_EZE, ID_MIA, inicio, fin, "PRECIO_ASC", pageable))
                .thenReturn(paginaConResultados);

        final org.springframework.data.domain.Page<InstanciaVuelo> res =
                flightService.buscarVuelosPublicos(
                        ID_EZE, ID_MIA, inicio, fin, "PRECIO_ASC", pageable);

        assertEquals(1, res.getContent().size());
    }
}