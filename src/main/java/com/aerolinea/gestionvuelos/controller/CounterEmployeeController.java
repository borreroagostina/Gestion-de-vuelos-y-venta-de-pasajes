package com.aerolinea.gestionvuelos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CounterEmployeeController {

  @GetMapping("/empleado")
  public String counterEmployeeHome(Model model) {
    model.addAttribute("title", "Panel del empleado de mostrador");
    model.addAttribute("subtitle", "Atencion, reservas y ventas de pasajes");
    return "empleado";
  }
}
