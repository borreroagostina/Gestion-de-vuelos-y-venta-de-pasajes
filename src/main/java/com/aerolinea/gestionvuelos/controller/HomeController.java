package com.aerolinea.gestionvuelos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("title", "Gestion de vuelos");
    model.addAttribute("message", "Sistema de aerolinea iniciado correctamente");
    return "index";
  }
}
