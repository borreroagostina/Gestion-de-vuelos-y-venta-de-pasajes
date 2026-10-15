package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.dto.FormValidationIssue;
import com.aerolinea.gestionvuelos.dto.PasajerosReservaForm;
import com.aerolinea.gestionvuelos.dto.ReservaCheckoutContext;
import com.aerolinea.gestionvuelos.dto.ResumenCompraDto;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.CheckoutService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pasajero/checkout")
@PreAuthorize("hasRole('PASAJERO')")
public class CheckoutController {

  private static final String SESSION_FORM_KEY = "checkoutPasajerosForm";

  private final UsuarioRepository usuarioRepository;
  private final CheckoutService checkoutService;

  public CheckoutController(UsuarioRepository usuarioRepository, CheckoutService checkoutService) {
    this.usuarioRepository = usuarioRepository;
    this.checkoutService = checkoutService;
  }

  @GetMapping("/pasajeros")
  public String cargarPasajeros(
      Authentication authentication,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes) {
    final Optional<ReservaCheckoutContext> reservaOpt =
        obtenerReservaActiva(authentication, session);
    if (reservaOpt.isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "error", "No hay una reserva temporal activa para completar la compra.");
      return "redirect:/pasajero";
    }

    final ReservaCheckoutContext reserva = reservaOpt.get();
    final PasajerosReservaForm form =
        obtenerFormularioDesdeSesion(session, reserva.cantidadAsientos());
    model.addAttribute("pasajerosForm", form);
    cargarDatosBase(model, reserva);
    return "checkout-pasajeros";
  }

  @PostMapping("/resumen")
  public String generarResumen(
      @Valid @ModelAttribute("pasajerosForm") PasajerosReservaForm pasajerosForm,
      BindingResult bindingResult,
      Authentication authentication,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes) {
    final Optional<ReservaCheckoutContext> reservaOpt =
        obtenerReservaActiva(authentication, session);
    if (reservaOpt.isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "error", "La reserva temporal expiró o ya no está disponible.");
      return "redirect:/pasajero";
    }

    final ReservaCheckoutContext reserva = reservaOpt.get();
    final List<FormValidationIssue> issues =
        checkoutService.validarPasajeros(pasajerosForm.getPasajeros(), reserva.cantidadAsientos());
    for (FormValidationIssue issue : issues) {
      bindingResult.rejectValue(issue.fieldPath(), "invalid", issue.message());
    }

    if (bindingResult.hasErrors()) {
      cargarDatosBase(model, reserva);
      return "checkout-pasajeros";
    }

    session.setAttribute(SESSION_FORM_KEY, pasajerosForm);
    return "redirect:/pasajero/checkout/resumen";
  }

  @GetMapping("/resumen")
  public String verResumen(
      Authentication authentication,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes) {
    final Optional<ReservaCheckoutContext> reservaOpt =
        obtenerReservaActiva(authentication, session);
    if (reservaOpt.isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "error", "La reserva temporal expiró o ya no está disponible.");
      return "redirect:/pasajero";
    }

    final ReservaCheckoutContext reserva = reservaOpt.get();
    final Object formEnSesion = session.getAttribute(SESSION_FORM_KEY);
    if (!(formEnSesion instanceof PasajerosReservaForm form) || form.getPasajeros() == null) {
      redirectAttributes.addFlashAttribute(
          "error", "Debes completar los datos de los pasajeros antes de ver el resumen.");
      return "redirect:/pasajero/checkout/pasajeros";
    }

    final List<FormValidationIssue> issues =
        checkoutService.validarPasajeros(form.getPasajeros(), reserva.cantidadAsientos());
    if (!issues.isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "error", "Hay datos inválidos de pasajeros. Revisá el formulario.");
      return "redirect:/pasajero/checkout/pasajeros";
    }

    final ResumenCompraDto resumen = checkoutService.construirResumen(reserva, form.getPasajeros());
    model.addAttribute("reserva", reserva);
    model.addAttribute("resumen", resumen);
    model.addAttribute(
        "minutosRestantes", checkoutService.minutosRestantes(reserva.fechaExpiracion()));
    return "checkout-resumen";
  }

  private Optional<ReservaCheckoutContext> obtenerReservaActiva(
      Authentication authentication, HttpSession session) {
    if (authentication == null) {
      return Optional.empty();
    }
    final Usuario usuario =
        usuarioRepository
            .findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    final String sessionId = session != null ? session.getId() : null;
    return checkoutService.obtenerReservaActiva(usuario.getId(), sessionId);
  }

  private PasajerosReservaForm obtenerFormularioDesdeSesion(
      HttpSession session, int cantidadEsperada) {
    final Object formEnSesion = session.getAttribute(SESSION_FORM_KEY);
    if (formEnSesion instanceof PasajerosReservaForm form
        && form.getPasajeros() != null
        && form.getPasajeros().size() == cantidadEsperada) {
      return form;
    }
    final PasajerosReservaForm nuevo = PasajerosReservaForm.crearParaCantidad(cantidadEsperada);
    session.setAttribute(SESSION_FORM_KEY, nuevo);
    return nuevo;
  }

  private void cargarDatosBase(Model model, ReservaCheckoutContext reserva) {
    model.addAttribute("reserva", reserva);
    model.addAttribute(
        "minutosRestantes", checkoutService.minutosRestantes(reserva.fechaExpiracion()));
  }
}
