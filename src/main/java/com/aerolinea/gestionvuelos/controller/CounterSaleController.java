package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.dto.CounterSaleForm;
import com.aerolinea.gestionvuelos.dto.PasajeroViajeForm;
import com.aerolinea.gestionvuelos.dto.ReservaCheckoutContext;
import com.aerolinea.gestionvuelos.model.Asiento;
import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import com.aerolinea.gestionvuelos.model.MetodoPagoMostrador;
import com.aerolinea.gestionvuelos.model.Usuario;
import com.aerolinea.gestionvuelos.repository.UsuarioRepository;
import com.aerolinea.gestionvuelos.service.BookingService;
import com.aerolinea.gestionvuelos.service.CatalogService;
import com.aerolinea.gestionvuelos.service.CheckoutService;
import com.aerolinea.gestionvuelos.service.CounterSaleService;
import com.aerolinea.gestionvuelos.service.CounterSaleService.CounterSaleResult;
import com.aerolinea.gestionvuelos.service.FarePolicyService;
import com.aerolinea.gestionvuelos.service.FlightService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

@Controller
@RequestMapping("/empleado/venta")
@PreAuthorize("hasAnyRole('EMPLEADO','ADMIN')")
public class CounterSaleController {

  private static final Logger LOGGER = LoggerFactory.getLogger(CounterSaleController.class);
  private static final int RESULT_PAGE_SIZE = 10;
  private static final int LOW_AVAILABILITY_THRESHOLD = 5;

  private final UsuarioRepository userRepository;
  private final FlightService flightService;
  private final CatalogService catalogService;
  private final BookingService bookingService;
  private final CheckoutService checkoutService;
  private final CounterSaleService counterSaleService;
  private final FarePolicyService farePolicyService;

  public CounterSaleController(
      UsuarioRepository userRepository,
      FlightService flightService,
      CatalogService catalogService,
      BookingService bookingService,
      CheckoutService checkoutService,
      CounterSaleService counterSaleService,
      FarePolicyService farePolicyService) {
    this.userRepository = userRepository;
    this.flightService = flightService;
    this.catalogService = catalogService;
    this.bookingService = bookingService;
    this.checkoutService = checkoutService;
    this.counterSaleService = counterSaleService;
    this.farePolicyService = farePolicyService;
  }

  @GetMapping
  public String buscar(
      @RequestParam(required = false) Long origenId,
      @RequestParam(required = false) Long destinoId,
      @RequestParam(required = false) String fechaSalida,
      Model model) {
    model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
    model.addAttribute("origenId", origenId);
    model.addAttribute("destinoId", destinoId);
    model.addAttribute("fechaSalida", fechaSalida);
    if (origenId != null && destinoId != null && fechaSalida != null && !fechaSalida.isBlank()) {
      try {
        final LocalDate salida = LocalDate.parse(fechaSalida);
        model.addAttribute(
            "resultados",
            flightService
                .buscarVuelosPublicos(
                    origenId,
                    destinoId,
                    salida,
                    salida,
                    "HORA_ASC",
                    PageRequest.of(0, RESULT_PAGE_SIZE))
                .getContent());
      } catch (IllegalArgumentException exception) {
        model.addAttribute("error", exception.getMessage());
      }
    }
    return "empleado-venta";
  }

  @GetMapping("/vuelos/{id}")
  public String asientos(@PathVariable Long id, Model model) {
    final InstanciaVuelo instance = flightService.obtenerInstancia(id);
    if (instance == null || instance.isCancelada()) {
      return "redirect:/empleado/venta?error=Vuelo no encontrado o cancelado";
    }
    final List<Asiento> seats = bookingService.listarAsientosDeInstancia(id);
    model.addAttribute("ventaMostrador", Boolean.TRUE);
    model.addAttribute("vuelo", instance);
    model.addAttribute("dispEconomy", bookingService.contarDisponibles(id, "ECONOMY"));
    model.addAttribute("dispPrimera", bookingService.contarDisponibles(id, "PRIMERA"));
    model.addAttribute(
        "asientosPrimera", seats.stream().filter(a -> "PRIMERA".equals(a.getClase())).toList());
    model.addAttribute(
        "asientosEconomy", seats.stream().filter(a -> "ECONOMY".equals(a.getClase())).toList());
    model.addAttribute("umbralBajaDisp", LOW_AVAILABILITY_THRESHOLD);
    model.addAttribute("politicas", farePolicyService.obtenerPoliticas());
    return "vuelo-detalle";
  }

  @PostMapping("/vuelos/{id}/reservar")
  public String reservar(
      @PathVariable Long id,
      @RequestParam("asientoIds") List<Long> asientoIds,
      @RequestParam(name = "claseSeleccionada", required = false) String claseSeleccionada,
      Authentication authentication,
      HttpSession session,
      RedirectAttributes redirectAttributes) {
    try {
      bookingService.reservarAsientos(
          currentUser(authentication).getId(), session.getId(), asientoIds, claseSeleccionada);
      return "redirect:/empleado/venta/cliente";
    } catch (IllegalArgumentException | IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("error", exception.getMessage());
      return "redirect:/empleado/venta/vuelos/" + id;
    }
  }

  @GetMapping("/cliente")
  public String datosCliente(
      Authentication authentication,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes) {
    final Optional<ReservaCheckoutContext> reservation = activeReservation(authentication, session);
    if (reservation.isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "error", "No hay asientos reservados. Elegí un vuelo y asientos para vender.");
      return "redirect:/empleado/venta";
    }
    if (!model.containsAttribute("ventaForm")) {
      model.addAttribute(
          "ventaForm", CounterSaleForm.crearParaCantidad(reservation.get().cantidadAsientos()));
    }
    loadBase(model, reservation.get());
    return "empleado-venta-cliente";
  }

  @PostMapping("/confirmar")
  public String confirmar(
      @Valid @ModelAttribute("ventaForm") CounterSaleForm form,
      BindingResult bindingResult,
      Authentication authentication,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes) {
    final Usuario employee = currentUser(authentication);
    final Optional<ReservaCheckoutContext> reservation = activeReservation(authentication, session);
    if (reservation.isEmpty()) {
      redirectAttributes.addFlashAttribute("error", "La reserva temporal expiró o no existe.");
      return "redirect:/empleado/venta";
    }
    final List<PasajeroViajeForm> passengers = form.getPasajeros();
    checkoutService
        .validarPasajeros(passengers, reservation.get().cantidadAsientos())
        .forEach(issue -> bindingResult.rejectValue(issue.fieldPath(), "invalid", issue.message()));
    if (!bindingResult.hasErrors()) {
      try {
        final CounterSaleResult result =
            counterSaleService.registerSale(employee.getId(), reservation.get().reservaId(), form);
        redirectAttributes.addFlashAttribute("venta", result);
        return "redirect:/empleado/venta/confirmacion";
      } catch (IllegalArgumentException | IllegalStateException exception) {
        LOGGER.warn("No se pudo registrar la venta de mostrador.", exception);
        model.addAttribute("error", exception.getMessage());
      }
    }
    loadBase(model, reservation.get());
    return "empleado-venta-cliente";
  }

  @GetMapping("/confirmacion")
  public String confirmacion(Model model) {
    if (!model.containsAttribute("venta")) {
      return "redirect:/empleado/venta";
    }
    return "empleado-venta-confirmacion";
  }

  private void loadBase(Model model, ReservaCheckoutContext reservation) {
    model.addAttribute("reserva", reservation);
    model.addAttribute(
        "resumen",
        checkoutService.construirResumen(
            reservation,
            CounterSaleForm.crearParaCantidad(reservation.cantidadAsientos()).getPasajeros()));
    model.addAttribute(
        "minutosRestantes", checkoutService.minutosRestantes(reservation.fechaExpiracion()));
    model.addAttribute("metodosPago", MetodoPagoMostrador.values());
  }

  private Optional<ReservaCheckoutContext> activeReservation(
      Authentication authentication, HttpSession session) {
    return checkoutService.obtenerReservaActiva(
        currentUser(authentication).getId(), session.getId());
  }

  private Usuario currentUser(Authentication authentication) {
    return userRepository
        .findByEmailIgnoreCase(authentication.getName())
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
  }
}
