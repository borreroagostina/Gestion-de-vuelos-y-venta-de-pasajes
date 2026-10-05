package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.VueloProgramado;
import com.aerolinea.gestionvuelos.repository.AeronaveRepository;
import com.aerolinea.gestionvuelos.repository.AeropuertoRepository;
import com.aerolinea.gestionvuelos.repository.InstanciaVueloRepository;
import com.aerolinea.gestionvuelos.repository.VueloProgramadoRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para administracion de vuelos (US-05, US-06) y busqueda publica (US-08).
 */
@Service
public class FlightService {

    private final VueloProgramadoRepository vueloRepo;
    private final InstanciaVueloRepository instanciaRepo;
    private final AeropuertoRepository aeroRepo;
    private final AeronaveRepository aeronaveRepo;
    private final JdbcTemplate jdbcTemplate;

    public FlightService(
            final VueloProgramadoRepository vueloRepo,
            final InstanciaVueloRepository instanciaRepo,
            final AeropuertoRepository aeroRepo,
            final AeronaveRepository aeronaveRepo,
            final JdbcTemplate jdbcTemplate) {
        this.vueloRepo = vueloRepo;
        this.instanciaRepo = instanciaRepo;
        this.aeroRepo = aeroRepo;
        this.aeronaveRepo = aeronaveRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<VueloProgramado> listarVuelosProgramados() {
        return vueloRepo.findAllByOrderByCodigoVueloAsc();
    }

    public List<VueloProgramado> filtrarVuelosAdmin(
            final Long origenId,
            final Long destinoId,
            final LocalDate fechaDesde,
            final LocalDate fechaHasta) {
        return vueloRepo.filtrarVuelos(origenId, destinoId, fechaDesde, fechaHasta);
    }

    public VueloProgramado obtenerVuelo(final Long id) {
        return vueloRepo.findById(id).orElse(null);
    }

    @Transactional
    public VueloProgramado crearVuelo(final VueloProgramado vuelo) {
        validarReglasNegocio(vuelo);
        vuelo.setCodigoVuelo(vuelo.getCodigoVuelo().trim().toUpperCase(Locale.ROOT));

        if (vueloRepo.existsByCodigoVueloIgnoreCase(vuelo.getCodigoVuelo())) {
            throw new IllegalArgumentException("Ya existe un vuelo con el codigo ingresado");
        }

        final VueloProgramado guardado = vueloRepo.save(vuelo);
        generarInstancias(guardado, guardado.getFechaInicioVenta(), guardado.getFechaFinVenta());
        return guardado;
    }

    @Transactional
    public VueloProgramado modificarVuelo(final VueloProgramado vuelo) {
        if (vuelo == null || vuelo.getId() == null) {
            throw new IllegalArgumentException("Debe especificar un vuelo valido para modificar");
        }
        validarReglasNegocio(vuelo);
        vuelo.setCodigoVuelo(vuelo.getCodigoVuelo().trim().toUpperCase(Locale.ROOT));

        if (vueloRepo.existsByCodigoVueloIgnoreCaseAndIdNot(vuelo.getCodigoVuelo(), vuelo.getId())) {
            throw new IllegalArgumentException("Ya existe un vuelo con el codigo ingresado");
        }

        final VueloProgramado existente =
                vueloRepo
                        .findById(vuelo.getId())
                        .orElseThrow(() -> new IllegalArgumentException("El vuelo no existe"));

        final LocalDate hoy = LocalDate.now();
        final List<InstanciaVuelo> futuras =
                instanciaRepo.findByVueloProgramadoIdAndFechaSalidaGreaterThanEqual(existente.getId(), hoy);

        // Validación: no permitir reducir asientos por debajo de los ya vendidos
        for (final InstanciaVuelo inst : futuras) {
            final int vendidosEconomy = calcularAsientosVendidos(inst.getId(), "ECONOMY");
            final int vendidosPrimera = calcularAsientosVendidos(inst.getId(), "PRIMERA");

            if (vuelo.getAeronave().getEconomySeats() < vendidosEconomy) {
                throw new IllegalStateException(
                        "No se puede reducir la capacidad Economy por debajo de los asientos vendidos ("
                                + vendidosEconomy
                                + ")");
            }
            if (vuelo.getAeronave().getPrimeraClaseSeats() < vendidosPrimera) {
                throw new IllegalStateException(
                        "No se puede reducir la capacidad Primera Clase por debajo de los asientos vendidos ("
                                + vendidosPrimera
                                + ")");
            }
        }

        // Actualización de campos del vuelo programado
        existente.setCodigoVuelo(vuelo.getCodigoVuelo());
        existente.setOrigen(vuelo.getOrigen());
        existente.setDestino(vuelo.getDestino());
        existente.setHoraSalida(vuelo.getHoraSalida());
        existente.setHoraLlegada(vuelo.getHoraLlegada());
        existente.setDiasSemana(vuelo.getDiasSemana());
        existente.setPrecioEconomy(vuelo.getPrecioEconomy());
        existente.setPrecioPrimera(vuelo.getPrecioPrimera());
        existente.setAeronave(vuelo.getAeronave());
        existente.setFechaInicioVenta(vuelo.getFechaInicioVenta());
        existente.setFechaFinVenta(vuelo.getFechaFinVenta());

        final VueloProgramado actualizado = vueloRepo.save(existente);

        // Eliminar instancias futuras que no tengan ventas registradas
        for (final InstanciaVuelo inst : futuras) {
            final int vendidosEco = calcularAsientosVendidos(inst.getId(), "ECONOMY");
            final int vendidosPri = calcularAsientosVendidos(inst.getId(), "PRIMERA");
            if (vendidosEco == 0 && vendidosPri == 0) {
                instanciaRepo.delete(inst);
            }
        }

        // Regenerar las instancias futuras respetando el nuevo período y días
        final LocalDate desde =
                actualizado.getFechaInicioVenta().isAfter(hoy)
                        ? actualizado.getFechaInicioVenta()
                        : hoy;

        if (!desde.isAfter(actualizado.getFechaFinVenta())) {
            generarInstancias(actualizado, desde, actualizado.getFechaFinVenta());
        }

        return actualizado;
    }

    public Page<InstanciaVuelo> buscarVuelosPublicos(
            final Long origenId,
            final Long destinoId,
            final LocalDate fechaSalida,
            final LocalDate fechaHasta,
            final String orden,
            final Pageable pageable) {

        if (origenId == null || destinoId == null) {
            throw new IllegalArgumentException("Debe seleccionar origen y destino");
        }
        if (origenId.equals(destinoId)) {
            throw new IllegalArgumentException("El aeropuerto de origen y destino no pueden coincidir");
        }
        final LocalDate hoy = LocalDate.now();
        if (fechaSalida == null || fechaSalida.isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de consulta debe ser igual o posterior a hoy");
        }
        final LocalDate fin = (fechaHasta != null) ? fechaHasta : fechaSalida;
        if (fin.isBefore(fechaSalida)) {
            throw new IllegalArgumentException("La fecha 'hasta' no puede ser anterior a la de partida");
        }

        return instanciaRepo.buscarVuelosPublicos(
                origenId, destinoId, fechaSalida, fin, orden, pageable);
    }

    private void generarInstancias(
            final VueloProgramado vuelo, final LocalDate inicio, final LocalDate fin) {
        final List<DayOfWeek> diasPermitidos = parsearDiasSemana(vuelo.getDiasSemana());
        final List<InstanciaVuelo> lista = new ArrayList<>();

        LocalDate cursor = inicio;
        while (!cursor.isAfter(fin)) {
            if (diasPermitidos.contains(cursor.getDayOfWeek())) {
                final InstanciaVuelo inst = new InstanciaVuelo();
                inst.setVueloProgramado(vuelo);
                inst.setFechaSalida(cursor);
                inst.setHoraSalida(vuelo.getHoraSalida());
                inst.setHoraLlegada(vuelo.getHoraLlegada());
                inst.setPrecioEconomy(vuelo.getPrecioEconomy());
                inst.setPrecioPrimera(vuelo.getPrecioPrimera());

                // Cada instancia arranca con la capacidad total de la aeronave
                inst.setAsientosDispEconomy(vuelo.getAeronave().getEconomySeats());
                inst.setAsientosDispPrimera(vuelo.getAeronave().getPrimeraClaseSeats());

                // Cruce de medianoche: llegada al dia siguiente
                if (vuelo.llegaAlDiaSiguiente()) {
                    inst.setFechaLlegada(cursor.plusDays(1));
                } else {
                    inst.setFechaLlegada(cursor);
                }
                lista.add(inst);
            }
            cursor = cursor.plusDays(1);
        }
        instanciaRepo.saveAll(lista);
    }

    public void validarReglasNegocio(final VueloProgramado vuelo) {
        if (vuelo == null) {
            throw new IllegalArgumentException("El vuelo no puede ser nulo");
        }
        if (vuelo.getOrigen() == null || vuelo.getDestino() == null) {
            throw new IllegalArgumentException("Debe ingresar aeropuertos de origen y destino");
        }
        if (vuelo.getOrigen().getId() != null
                && vuelo.getOrigen().getId().equals(vuelo.getDestino().getId())) {
            throw new IllegalArgumentException(
                    "El aeropuerto de origen no puede coincidir con el destino");
        }
        if (vuelo.getFechaInicioVenta() == null || vuelo.getFechaFinVenta() == null) {
            throw new IllegalArgumentException("Las fechas del período de venta son obligatorias");
        }
        if (vuelo.getFechaFinVenta().isBefore(vuelo.getFechaInicioVenta())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }
        if (vuelo.getDiasSemana() == null || vuelo.getDiasSemana().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un dia de la semana");
        }
        if (vuelo.getPrecioEconomy() == null
                || vuelo.getPrecioEconomy().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio Economy debe ser positivo");
        }
        if (vuelo.getPrecioPrimera() == null
                || vuelo.getPrecioPrimera().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio Primera Clase debe ser positivo");
        }
    }

    private List<DayOfWeek> parsearDiasSemana(final String diasCadena) {
        if (diasCadena == null || diasCadena.isBlank()) {
            return new ArrayList<>();
        }
        final List<DayOfWeek> dias = new ArrayList<>();
        for (String item : diasCadena.split(",")) {
            final String token = item.trim().toUpperCase(Locale.ROOT)
                    .replace("É", "E")
                    .replace("Á", "A");

            switch (token) {
                case "MONDAY":
                case "LUNES":
                    dias.add(DayOfWeek.MONDAY);
                    break;
                case "TUESDAY":
                case "MARTES":
                    dias.add(DayOfWeek.TUESDAY);
                    break;
                case "WEDNESDAY":
                case "MIERCOLES":
                    dias.add(DayOfWeek.WEDNESDAY);
                    break;
                case "THURSDAY":
                case "JUEVES":
                    dias.add(DayOfWeek.THURSDAY);
                    break;
                case "FRIDAY":
                case "VIERNES":
                    dias.add(DayOfWeek.FRIDAY);
                    break;
                case "SATURDAY":
                case "SABADO":
                    dias.add(DayOfWeek.SATURDAY);
                    break;
                case "SUNDAY":
                case "DOMINGO":
                    dias.add(DayOfWeek.SUNDAY);
                    break;
                default:
                    break;
            }
        }
        return dias;
    }

    private int calcularAsientosVendidos(final Long instanciaId, final String clase) {
        final String query =
                "SELECT COUNT(*) FROM ventas WHERE vuelo_id = ? AND UPPER(clase) = ?";
        try {
            final Integer cant = jdbcTemplate.queryForObject(query, Integer.class, instanciaId, clase);
            return cant != null ? cant : 0;
        } catch (final Exception e) {
            return 0;
        }
    }
}