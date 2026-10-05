package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.service.CatalogService;
import com.aerolinea.gestionvuelos.service.FlightService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Portada y Buscador Publico de vuelos sin requerir autenticacion (US-08).
 */
@Controller
public class HomeController {

  private static final int RESULT_PAGE_SIZE = 10;

  private final FlightService flightService;
  private final CatalogService catalogService;

  public HomeController(final FlightService flightService, final CatalogService catalogService) {
    this.flightService = flightService;
    this.catalogService = catalogService;
  }

  @GetMapping("/")
  public String home(
          final Model model,
          @RequestParam(required = false) final Long origenId,
          @RequestParam(required = false) final Long destinoId,
          @RequestParam(required = false) final String fechaSalida,
          @RequestParam(required = false) final String fechaHasta,
          @RequestParam(defaultValue = "HORA_ASC") final String orden,
          @RequestParam(defaultValue = "0") final int page) {

    model.addAttribute("title", "Gestion de Vuelos");
    model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
    model.addAttribute("origenId", origenId);
    model.addAttribute("destinoId", destinoId);
    model.addAttribute("fechaSalida", fechaSalida);
    model.addAttribute("fechaHasta", fechaHasta);
    model.addAttribute("orden", orden);

    if (origenId != null && destinoId != null && fechaSalida != null && !fechaSalida.isBlank()) {
      try {
        final LocalDate salida = LocalDate.parse(fechaSalida);
        final LocalDate hasta =
                (fechaHasta != null && !fechaHasta.isBlank()) ? LocalDate.parse(fechaHasta) : salida;

        final Page<InstanciaVuelo> resultados =
                flightService.buscarVuelosPublicos(
                        origenId,
                        destinoId,
                        salida,
                        hasta,
                        orden,
                        PageRequest.of(Math.max(0, page), RESULT_PAGE_SIZE));

        model.addAttribute("resultados", resultados.getContent());
        model.addAttribute("totalPages", resultados.getTotalPages());
        model.addAttribute("currentPage", resultados.getNumber());
        model.addAttribute("busquedaRealizada", Boolean.TRUE);
      } catch (final IllegalArgumentException e) {
        model.addAttribute("error", e.getMessage());
      }
    }
    return "index";
  }
}