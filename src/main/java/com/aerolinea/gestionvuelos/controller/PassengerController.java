package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.RolUsuario;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('PASAJERO')")
public class PassengerController {

  @GetMapping("/pasajero")
  public String passengerHome(Model model) {
    model.addAttribute("title", "Panel del pasajero");
    model.addAttribute("subtitle", "Busqueda, compra y gestion de viajes");
    model.addAttribute("permisos", RolUsuario.PASAJERO.getPermisos());
    model.addAttribute("compraPath", "/");
    return "pasajero";
  }
}
