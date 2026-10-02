package com.aerolinea.gestionvuelos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

  @GetMapping("/admin")
  public String adminHome(Model model) {
    model.addAttribute("title", "Panel de administracion");
    model.addAttribute("subtitle", "Gestion de vuelos, capacidad y reportes");
    return "admin";
  }
}
