package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.RolUsuario;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('EMPLEADO')")
public class CounterEmployeeController {

  @GetMapping("/empleado")
  public String counterEmployeeHome(Model model) {
    model.addAttribute("title", "Panel del empleado de mostrador");
    model.addAttribute("subtitle", "Atencion, reservas y ventas de pasajes");
    model.addAttribute("permisos", RolUsuario.EMPLEADO.getPermisos());
    return "empleado";
  }
}
