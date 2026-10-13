package com.aerolinea.gestionvuelos;

import com.aerolinea.gestionvuelos.model.*;
import com.aerolinea.gestionvuelos.repository.*;
import com.aerolinea.gestionvuelos.service.BookingService;
import com.aerolinea.gestionvuelos.service.FlightService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class USIntegrationTest {

    @Autowired
    private FlightService flightService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VueloProgramadoRepository vueloRepo;

    @Autowired
    private InstanciaVueloRepository instanciaRepo;

    @Autowired
    private AeropuertoRepository aeroRepo;

    @Autowired
    private AeronaveRepository aeronaveRepo;

    @Autowired
    private AsientoRepository asientoRepo;
    
    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private ReservaAsientoRepository reservaAsientoRepo;

    @Autowired
    private ReservaTemporalRepository reservaRepo;

    private Aeropuerto eze;
    private Aeropuerto cor;
    private Aeronave boeing;
    private Usuario testUser;

    @Before
    public void setup() {
        if (aeroRepo.count() == 0) {
            eze = new Aeropuerto("EZE", "Ezeiza", "Buenos Aires", "Argentina");
            eze = aeroRepo.save(eze);
            cor = new Aeropuerto("COR", "Pajas Blancas", "Córdoba", "Argentina");
            cor = aeroRepo.save(cor);
        } else {
            eze = aeroRepo.findAll().get(0);
            cor = aeroRepo.findAll().get(1);
        }

        if (aeronaveRepo.count() == 0) {
            boeing = new Aeronave("Boeing 737", "Boeing", "B737", 150, 20);
            boeing = aeronaveRepo.save(boeing);
        } else {
            boeing = aeronaveRepo.findAll().get(0);
        }
        
        testUser = new Usuario();
        testUser.setEmail("test@test.com");
        testUser.setNombre("Test");
        testUser.setApellido("User");
        testUser.setDocumento("12345678");
        testUser.setTelefono("11223344");
        testUser.setPasswordHash("pass");
        testUser.setRol(RolUsuario.PASAJERO);
        testUser.setActivo(true);
        if (!usuarioRepo.existsByEmailIgnoreCase(testUser.getEmail())) {
            testUser = usuarioRepo.save(testUser);
        } else {
            testUser = usuarioRepo.findByEmailIgnoreCase(testUser.getEmail()).get();
        }
    }

    @Test
    @Transactional
    public void testUS07_CancelacionVuelo() {
        // Crear un vuelo
        VueloProgramado v = new VueloProgramado();
        v.setCodigoVuelo("TEST07");
        v.setOrigen(eze);
        v.setDestino(cor);
        v.setAeronave(boeing);
        v.setHoraSalida(LocalTime.of(10, 0));
        v.setHoraLlegada(LocalTime.of(12, 0));
        v.setFechaInicioVenta(LocalDate.now().plusDays(1));
        v.setFechaFinVenta(LocalDate.now().plusDays(10));
        v.setDiasSemana("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
        v.setPrecioEconomy(new BigDecimal("1000"));
        v.setPrecioPrimera(new BigDecimal("2000"));

        v = flightService.crearVuelo(v);
        Long vueloId = v.getId();

        List<InstanciaVuelo> instancias = flightService.listarInstanciasDeVuelo(vueloId);
        Assert.assertFalse(instancias.isEmpty());
        InstanciaVuelo primera = instancias.get(0);

        // Cancelar fecha puntual
        flightService.cancelarInstanciaVuelo(primera.getId(), "Tormenta");
        InstanciaVuelo cancelada = flightService.obtenerInstancia(primera.getId());
        Assert.assertTrue(cancelada.isCancelada());
        Assert.assertEquals("Tormenta", cancelada.getMotivoCancelacion());

        // Verificar que no aparece en búsqueda
        Page<InstanciaVuelo> result = flightService.buscarVuelosPublicos(
                eze.getId(), cor.getId(), cancelada.getFechaSalida(), cancelada.getFechaSalida(), "HORA_ASC", PageRequest.of(0, 10));
        Assert.assertTrue(result.getContent().stream().noneMatch(i -> i.getId().equals(cancelada.getId())));

        // Cancelar programación completa
        flightService.cancelarVueloProgramado(vueloId, "Fin de temporada");
        VueloProgramado vCancelado = flightService.obtenerVuelo(vueloId);
        Assert.assertTrue(vCancelado.isCancelado());

        List<InstanciaVuelo> todas = flightService.listarInstanciasDeVuelo(vueloId);
        for (InstanciaVuelo inst : todas) {
            Assert.assertTrue("Todas las instancias deben estar canceladas", inst.isCancelada());
        }
    }

    @Test
    @Transactional
    public void testUS10_LimiteNueveAsientos() {
        VueloProgramado v = crearVueloGenerico("TEST10");
        InstanciaVuelo inst = flightService.listarInstanciasDeVuelo(v.getId()).get(0);
        List<Asiento> asientos = bookingService.listarAsientosDeInstancia(inst.getId());
        
        List<Long> diezAsientos = asientos.stream().limit(10).map(Asiento::getId).collect(Collectors.toList());
        
        try {
            bookingService.reservarAsientos(testUser.getId(), "test-session-limit", diezAsientos);
            Assert.fail("Deberia haber fallado por limite de 9 asientos");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("No se pueden reservar mas de 9 asientos", e.getMessage());
        }
    }

    @Test
    public void testUS10_ConcurrenciaReserva() throws InterruptedException, ExecutionException {
        VueloProgramado v = crearVueloGenerico("CONCUR");
        InstanciaVuelo inst = flightService.listarInstanciasDeVuelo(v.getId()).get(0);
        List<Asiento> asientos = bookingService.listarAsientosDeInstancia(inst.getId());
        Asiento target = asientos.get(0);
        List<Long> targetId = List.of(target.getId());

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                try {
                    bookingService.reservarAsientos(testUser.getId(), "test-session-concurrency", targetId);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> results = executor.invokeAll(tasks);
        int successCount = 0;
        for (Future<Boolean> res : results) {
            if (res.get()) successCount++;
        }

        Assert.assertEquals("Solo uno deberia haber tenido exito", 1, successCount);
        executor.shutdown();
    }

    @Test
    @Transactional
    public void testUS10_NoPermiteMezclarClasesEnMismaOperacion() {
        VueloProgramado v = crearVueloGenerico("MIXCL");
        InstanciaVuelo inst = flightService.listarInstanciasDeVuelo(v.getId()).get(0);
        Asiento primera = asientoRepo.findByInstanciaVueloIdOrderByNumeroAsc(inst.getId()).stream()
                .filter(a -> "PRIMERA".equals(a.getClase()))
                .findFirst()
                .orElseThrow();
        Asiento economy = asientoRepo.findByInstanciaVueloIdOrderByNumeroAsc(inst.getId()).stream()
                .filter(a -> "ECONOMY".equals(a.getClase()))
                .findFirst()
                .orElseThrow();

        try {
            bookingService.reservarAsientos(
                    testUser.getId(),
                    "test-session-mixed",
                    List.of(primera.getId(), economy.getId()),
                    "PRIMERA");
            Assert.fail("Deberia rechazar seleccion mixta de clases");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(
                    "Debe seleccionar asientos de una sola clase por operación", e.getMessage());
        }
    }

    @Test
    @Transactional
    public void testUS10_NumeracionAsientos() {
        VueloProgramado v = crearVueloGenerico("NUM01");
        InstanciaVuelo inst = flightService.listarInstanciasDeVuelo(v.getId()).get(0);
        List<Asiento> asientos = bookingService.listarAsientosDeInstancia(inst.getId());

        List<Asiento> primera = asientos.stream()
                .filter(a -> "PRIMERA".equals(a.getClase()))
                .collect(Collectors.toList());
        List<Asiento> economy = asientos.stream()
                .filter(a -> "ECONOMY".equals(a.getClase()))
                .collect(Collectors.toList());

        Assert.assertFalse(primera.isEmpty());
        Assert.assertFalse(economy.isEmpty());

        // Verificar formato 01A, 01B...
        Assert.assertEquals("01A", primera.get(0).getNumero());
        Assert.assertEquals("01B", primera.get(1).getNumero());

        // Con 20 asientos de primera, son 5 filas (1-5).
        // La economia deberia empezar en la fila 7 (max(5+2, 5))
        Assert.assertEquals("07A", economy.get(0).getNumero());
    }

    private VueloProgramado crearVueloGenerico(String codigo) {
        return crearVueloConAeronave(codigo, boeing);
    }

    private VueloProgramado crearVueloConAeronave(String codigo, Aeronave aeronave) {
        VueloProgramado v = new VueloProgramado();
        v.setCodigoVuelo(codigo);
        v.setOrigen(eze);
        v.setDestino(cor);
        v.setAeronave(aeronave);
        v.setHoraSalida(LocalTime.of(10, 0));
        v.setHoraLlegada(LocalTime.of(12, 0));
        v.setFechaInicioVenta(LocalDate.now().plusDays(1));
        v.setFechaFinVenta(LocalDate.now().plusDays(2));
        v.setDiasSemana("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
        v.setPrecioEconomy(new BigDecimal("1000"));
        v.setPrecioPrimera(new BigDecimal("2000"));
        return flightService.crearVuelo(v);
    }

    @Test
    @Transactional
    public void testUS09_DisponibilidadPorClaseRestaReservasActivasYVendidos() {
        Aeronave aeronaveChica = new Aeronave("Mini Jet", "Test", "M" + System.nanoTime(), 6, 2);
        aeronaveChica = aeronaveRepo.save(aeronaveChica);

        VueloProgramado v = crearVueloConAeronave("US09DET", aeronaveChica);
        InstanciaVuelo inst = flightService.listarInstanciasDeVuelo(v.getId()).get(0);
        List<Asiento> asientos = bookingService.listarAsientosDeInstancia(inst.getId());

        Asiento ecoReservado = asientos.stream().filter(a -> "ECONOMY".equals(a.getClase())).findFirst().orElseThrow();
        List<Asiento> economy = asientos.stream().filter(a -> "ECONOMY".equals(a.getClase())).toList();
        Asiento ecoVendido = economy.get(1);

        List<Asiento> primera = asientos.stream().filter(a -> "PRIMERA".equals(a.getClase())).toList();
        Asiento priVendido1 = primera.get(0);
        Asiento priVendido2 = primera.get(1);

        bookingService.reservarAsientos(testUser.getId(), "test-session-us09", List.of(ecoReservado.getId()));

        ecoVendido.setEstado("OCUPADO");
        priVendido1.setEstado("OCUPADO");
        priVendido2.setEstado("OCUPADO");
        asientoRepo.saveAll(List.of(ecoVendido, priVendido1, priVendido2));

        long disponiblesEconomy = bookingService.contarDisponibles(inst.getId(), "ECONOMY");
        long disponiblesPrimera = bookingService.contarDisponibles(inst.getId(), "PRIMERA");

        Assert.assertEquals(4L, disponiblesEconomy);
        Assert.assertEquals(0L, disponiblesPrimera);
    }

    @Test
    @Transactional
    public void testUS10_LiberacionAutomaticaDeReservasExpiradas() {
        VueloProgramado v = crearVueloGenerico("US10EXP");
        InstanciaVuelo inst = flightService.listarInstanciasDeVuelo(v.getId()).get(0);
        Asiento asiento = bookingService.listarAsientosDeInstancia(inst.getId()).get(0);

        ReservaTemporal reserva =
                bookingService.reservarAsientos(
                        testUser.getId(), "test-session-expiration", List.of(asiento.getId()));
        reserva.setFechaExpiracion(java.time.LocalDateTime.now().minusMinutes(1));

        reservaRepo.save(reserva);
        bookingService.liberarReservasExpiradas();

        Asiento actualizado = asientoRepo.findById(asiento.getId()).orElseThrow();
        Assert.assertEquals("DISPONIBLE", actualizado.getEstado());

        boolean todosLiberados =
                reservaAsientoRepo.findByReservaTemporalId(reserva.getId()).stream()
                        .allMatch(r -> r.getEstado() == EstadoReservaAsiento.LIBERADO);
        Assert.assertTrue(todosLiberados);
    }
}
