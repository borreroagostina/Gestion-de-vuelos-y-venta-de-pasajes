package com.aerolinea.gestionvuelos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PassengerController {

  @GetMapping("/pasajero")
  public String passengerHome(Model model) {
    model.addAttribute("title", "Panel del pasajero");
    model.addAttribute("subtitle", "Busqueda, compra y gestion de viajes");
    return "pasajero";
  }
}
