package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.model.DiaSemana;
import com.aerolinea.gestionvuelos.model.VueloForm;
import com.aerolinea.gestionvuelos.model.VueloProgramado;
import com.aerolinea.gestionvuelos.service.CatalogService;
import com.aerolinea.gestionvuelos.service.FlightService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestion administrativa para alta y edicion de vuelos (US-05, US-06).
 */
@Controller
@RequestMapping("/admin/vuelos")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFlightController {

    private final FlightService flightService;
    private final CatalogService catalogService;

    public AdminFlightController(
            final FlightService flightService, final CatalogService catalogService) {
        this.flightService = flightService;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String listarVuelos(
            final Model model,
            @RequestParam(required = false) final Long origenId,
            @RequestParam(required = false) final Long destinoId,
            @RequestParam(required = false) final String fechaDesde,
            @RequestParam(required = false) final String fechaHasta) {

        LocalDate desde = null;
        LocalDate hasta = null;

        if (fechaDesde != null && !fechaDesde.isBlank()) {
            desde = LocalDate.parse(fechaDesde);
        }
        if (fechaHasta != null && !fechaHasta.isBlank()) {
            hasta = LocalDate.parse(fechaHasta);
        }

        model.addAttribute(
                "vuelos", flightService.filtrarVuelosAdmin(origenId, destinoId, desde, hasta));
        model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
        model.addAttribute("origenId", origenId);
        model.addAttribute("destinoId", destinoId);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);

        return "admin/vuelos";
    }

    @GetMapping("/nuevo")
    public String formularioNuevoVuelo(final Model model) {
        cargarAtributosFormulario(model);
        model.addAttribute("vueloForm", new VueloForm());
        model.addAttribute("esEdicion", Boolean.FALSE);
        return "admin/vuelo-form";
    }

    @PostMapping("/guardar")
    public String guardarVuelo(
            @Valid @ModelAttribute("vueloForm") final VueloForm form,
            final BindingResult result,
            final Model model,
            final RedirectAttributes ra) {

        final boolean esEdicion = form.getId() != null && form.getId() > 0;

        if (result.hasErrors()) {
            cargarAtributosFormulario(model);
            model.addAttribute("esEdicion", esEdicion);
            return "admin/vuelo-form";
        }

        try {
            final VueloProgramado vuelo = convertirFormAEntidad(form);
            if (esEdicion) {
                vuelo.setId(form.getId());
                flightService.modificarVuelo(vuelo);
                ra.addFlashAttribute("success", "Vuelo actualizado en instancias futuras.");
            } else {
                vuelo.setId(null);
                flightService.crearVuelo(vuelo);
                ra.addFlashAttribute("success", "Vuelo e instancias generados correctamente.");
            }
            return "redirect:/admin/vuelos";
        } catch (final Exception e) {
            cargarAtributosFormulario(model);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("esEdicion", esEdicion);
            return "admin/vuelo-form";
        }
    }

    @GetMapping("/{id}/editar")
    public String formularioEditarVuelo(@PathVariable final Long id, final Model model) {
        final VueloProgramado vuelo = flightService.obtenerVuelo(id);
        if (vuelo == null) {
            return "redirect:/admin/vuelos";
        }
        cargarAtributosFormulario(model);

        final VueloForm form = new VueloForm();
        form.setId(vuelo.getId());
        form.setCodigoVuelo(vuelo.getCodigoVuelo());
        form.setOrigenId(vuelo.getOrigen().getId());
        form.setDestinoId(vuelo.getDestino().getId());
        form.setAeronaveId(vuelo.getAeronave().getId());
        form.setHoraSalida(vuelo.getHoraSalida());
        form.setHoraLlegada(vuelo.getHoraLlegada());
        form.setFechaInicioVenta(vuelo.getFechaInicioVenta());
        form.setFechaFinVenta(vuelo.getFechaFinVenta());
        form.setPrecioEconomy(vuelo.getPrecioEconomy());
        form.setPrecioPrimera(vuelo.getPrecioPrimera());

        if (vuelo.getDiasSemana() != null && !vuelo.getDiasSemana().isBlank()) {
            form.setDiasSeleccionados(Arrays.asList(vuelo.getDiasSemana().split(",")));
        }

        model.addAttribute("vueloForm", form);
        model.addAttribute("esEdicion", Boolean.TRUE);
        return "admin/vuelo-form";
    }

    private void cargarAtributosFormulario(final Model model) {
        model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
        model.addAttribute("aeronaves", catalogService.listarAeronaves());
        model.addAttribute("todosLosDias", DiaSemana.values());
    }

    private VueloProgramado convertirFormAEntidad(final VueloForm form) {
        final Aeropuerto origen = catalogService.obtenerAeropuerto(form.getOrigenId());
        final Aeropuerto destino = catalogService.obtenerAeropuerto(form.getDestinoId());
        final Aeronave aeronave = catalogService.obtenerAeronave(form.getAeronaveId());

        final VueloProgramado v = new VueloProgramado();
        v.setCodigoVuelo(form.getCodigoVuelo());
        v.setOrigen(origen);
        v.setDestino(destino);
        v.setAeronave(aeronave);
        v.setHoraSalida(form.getHoraSalida());
        v.setHoraLlegada(form.getHoraLlegada());
        v.setFechaInicioVenta(form.getFechaInicioVenta());
        v.setFechaFinVenta(form.getFechaFinVenta());
        v.setPrecioEconomy(form.getPrecioEconomy());
        v.setPrecioPrimera(form.getPrecioPrimera());
        v.setDiasSemana(String.join(",", form.getDiasSeleccionados()));
        return v;
    }
}