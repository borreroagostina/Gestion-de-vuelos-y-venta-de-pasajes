package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.dto.DatosPasajerosForm;
import com.aerolinea.gestionvuelos.dto.ResumenCompraView;
import com.aerolinea.gestionvuelos.model.RolUsuario;
import com.aerolinea.gestionvuelos.service.PassengerCheckoutService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('PASAJERO')")
public class PassengerController {

  private final PassengerCheckoutService passengerCheckoutService;

  public PassengerController(PassengerCheckoutService passengerCheckoutService) {
    this.passengerCheckoutService = passengerCheckoutService;
  }

  @GetMapping("/pasajero")
  public String passengerHome(Model model, Authentication authentication, HttpSession session) {
    model.addAttribute("title", "Panel del pasajero");
    model.addAttribute("subtitle", "Busqueda, compra y gestion de viajes");
    model.addAttribute("permisos", RolUsuario.PASAJERO.getPermisos());
    model.addAttribute("compraPath", "/");
    final String email = authentication != null ? authentication.getName() : "";
    final String sesionId = session != null ? session.getId() : null;
    model.addAttribute(
        "checkoutActivo", passengerCheckoutService.tieneReservaActiva(email, sesionId));
    return "pasajero";
  }

  @GetMapping("/pasajero/checkout")
  public String mostrarFormularioCheckout(
      Model model, Authentication authentication, HttpSession session, RedirectAttributes ra) {
    try {
      final String email = authentication.getName();
      final String sesionId = session != null ? session.getId() : null;
      if (!model.containsAttribute("datosPasajerosForm")) {
        model.addAttribute(
            "datosPasajerosForm", passengerCheckoutService.crearFormulario(email, sesionId));
      }
      cargarContextoCheckout(model, email, sesionId);
      return "pasajero-checkout";
    } catch (IllegalStateException | IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/pasajero";
    }
  }

  @PostMapping("/pasajero/checkout")
  public String guardarDatosPasajeros(
      @Valid @ModelAttribute("datosPasajerosForm") DatosPasajerosForm datosPasajerosForm,
      BindingResult bindingResult,
      Model model,
      Authentication authentication,
      HttpSession session,
      RedirectAttributes ra) {
    final String email = authentication.getName();
    final String sesionId = session != null ? session.getId() : null;

    if (bindingResult.hasErrors()) {
      cargarContextoCheckout(model, email, sesionId);
      return "pasajero-checkout";
    }

    try {
      passengerCheckoutService.guardarPasajeros(email, sesionId, datosPasajerosForm);
      return "redirect:/pasajero/resumen";
    } catch (IllegalStateException | IllegalArgumentException e) {
      model.addAttribute("error", e.getMessage());
      cargarContextoCheckout(model, email, sesionId);
      return "pasajero-checkout";
    }
  }

  @GetMapping("/pasajero/resumen")
  public String mostrarResumen(
      Model model, Authentication authentication, HttpSession session, RedirectAttributes ra) {
    try {
      final String email = authentication.getName();
      final String sesionId = session != null ? session.getId() : null;
      final ResumenCompraView resumen = passengerCheckoutService.obtenerResumen(email, sesionId);
      model.addAttribute("resumen", resumen);
      return "pasajero-resumen";
    } catch (IllegalStateException | IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/pasajero/checkout";
    }
  }

  private void cargarContextoCheckout(Model model, String email, String sesionId) {
    model.addAttribute("fechaMaxNacimiento", LocalDate.now().minusDays(1));
    model.addAttribute(
        "claseReserva", passengerCheckoutService.obtenerClaseReserva(email, sesionId));
    model.addAttribute(
        "asientosReserva", passengerCheckoutService.obtenerAsientos(email, sesionId));
  }
}
