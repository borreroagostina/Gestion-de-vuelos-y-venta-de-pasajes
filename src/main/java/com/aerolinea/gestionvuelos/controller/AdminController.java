package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.RolUsuario;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  @GetMapping("/admin")
  public String adminHome(Model model) {
    model.addAttribute("title", "Panel de administracion");
    model.addAttribute("subtitle", "Gestion de vuelos, capacidad y reportes");
    model.addAttribute("permisos", RolUsuario.ADMIN.getPermisos());
    return "admin";
  }
}
