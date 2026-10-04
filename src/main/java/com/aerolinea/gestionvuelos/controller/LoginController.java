package com.aerolinea.gestionvuelos.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

  @GetMapping("/login")
  public String login(Authentication authentication) {
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      return "redirect:/inicio";
    }
    return "login";
  }

  @GetMapping("/acceso-denegado")
  public String accessDenied() {
    return "acceso-denegado";
  }
}
