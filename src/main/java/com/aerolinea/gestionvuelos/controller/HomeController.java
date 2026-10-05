package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.service.CatalogService;
import com.aerolinea.gestionvuelos.service.FlightService;
import java.time.LocalDate;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador principal para portada, busqueda publica de vuelos (US-08)
 * y redireccion inicial segun rol autenticado (US-04).
 */
@Controller
public class HomeController {

  private static final int RESULT_PAGE_SIZE = 10;

  private final FlightService flightService;
  private final CatalogService catalogService;

  @Autowired
  public HomeController(
          @Autowired(required = false) final FlightService flightService,
          @Autowired(required = false) final CatalogService catalogService) {
    this.flightService = flightService;
    this.catalogService = catalogService;
  }

  /**
   * Portada publica y buscador de vuelos accesible sin login (US-08).
   */
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
    model.addAttribute(
            "message", "Ingresá con tu cuenta para acceder al inicio correspondiente a tu rol.");

    if (catalogService != null) {
      model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
    } else {
      model.addAttribute("aeropuertos", Collections.emptyList());
    }

    model.addAttribute("origenId", origenId);
    model.addAttribute("destinoId", destinoId);
    model.addAttribute("fechaSalida", fechaSalida);
    model.addAttribute("fechaHasta", fechaHasta);
    model.addAttribute("orden", orden);

    if (flightService != null
            && origenId != null
            && destinoId != null
            && fechaSalida != null
            && !fechaSalida.isBlank()) {
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

  /**
   * Redireccion al panel del usuario autenticado segun su rol (US-04).
   */
  @GetMapping("/inicio")
  public String roleHome(final Authentication authentication) {
    if (authentication == null) {
      return "redirect:/login";
    }
    return RolUsuario.fromAuthorities(authentication.getAuthorities())
            .map(RolUsuario::getHomePath)
            .map(path -> "redirect:" + path)
            .orElse("redirect:/");
  }
}