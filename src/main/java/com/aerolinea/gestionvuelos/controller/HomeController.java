package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.RolUsuario;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("title", "Gestion de vuelos");
    model.addAttribute(
        "message", "Ingresá con tu cuenta para acceder al inicio correspondiente a tu rol.");
    return "index";
  }

  @GetMapping("/inicio")
  public String roleHome(Authentication authentication) {
    if (authentication == null) {
      return "redirect:/login";
    }
    return RolUsuario.fromAuthorities(authentication.getAuthorities())
        .map(RolUsuario::getHomePath)
        .map(path -> "redirect:" + path)
        .orElse("redirect:/");
  }
}
