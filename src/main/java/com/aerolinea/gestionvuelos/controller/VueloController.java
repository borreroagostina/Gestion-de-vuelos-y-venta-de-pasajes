package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.BookingService;
import com.aerolinea.gestionvuelos.service.FarePolicyService;
import com.aerolinea.gestionvuelos.service.FlightService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vuelos")
public class VueloController {

  private final FlightService flightService;
  private final BookingService bookingService;
  private final UsuarioRepository usuarioRepo;
  private final FarePolicyService farePolicyService;

  public VueloController(
      FlightService flightService,
      BookingService bookingService,
      UsuarioRepository usuarioRepo,
      FarePolicyService farePolicyService) {
    this.flightService = flightService;
    this.bookingService = bookingService;
    this.usuarioRepo = usuarioRepo;
    this.farePolicyService = farePolicyService;
  }

  @GetMapping("/{id}")
  public String detalleVuelo(@PathVariable Long id, Model model) {
    final InstanciaVuelo instancia = flightService.obtenerInstancia(id);
    if (instancia == null || instancia.isCancelada()) {
      return "redirect:/?error=Vuelo no encontrado o cancelado";
    }

    final long dispEconomy = bookingService.contarDisponibles(id, "ECONOMY");
    final long dispPrimera = bookingService.contarDisponibles(id, "PRIMERA");

    final List<Asiento> asientos = bookingService.listarAsientosDeInstancia(id);
    final List<Asiento> asientosPrimera =
        asientos.stream().filter(a -> "PRIMERA".equals(a.getClase())).toList();
    final List<Asiento> asientosEconomy =
        asientos.stream().filter(a -> "ECONOMY".equals(a.getClase())).toList();

    model.addAttribute("vuelo", instancia);
    model.addAttribute("dispEconomy", dispEconomy);
    model.addAttribute("dispPrimera", dispPrimera);
    model.addAttribute("asientosPrimera", asientosPrimera);
    model.addAttribute("asientosEconomy", asientosEconomy);
    model.addAttribute("umbralBajaDisp", 5); // Ejemplo: menos de 5 es baja disponibilidad
    model.addAttribute("politicas", farePolicyService.obtenerPoliticas());

    return "vuelo-detalle";
  }

  @PostMapping("/{id}/reservar")
  @PreAuthorize("hasRole('PASAJERO')")
  public String reservarAsientos(
      @PathVariable Long id,
      @RequestParam("asientoIds") List<Long> asientoIds,
      @RequestParam(name = "claseSeleccionada", required = false) String claseSeleccionada,
      Authentication authentication,
      HttpSession session,
      RedirectAttributes ra) {

    if (authentication == null) {
      return "redirect:/login";
    }

    try {
      final Usuario usuario =
          usuarioRepo
              .findByEmailIgnoreCase(authentication.getName())
              .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

      bookingService.reservarAsientos(
          usuario.getId(), session != null ? session.getId() : null, asientoIds, claseSeleccionada);
      ra.addFlashAttribute(
          "success", "Asientos reservados temporalmente. Complete el pago para confirmar.");
      return "redirect:/pasajero/checkout";
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/vuelos/" + id;
    }
  }
}
