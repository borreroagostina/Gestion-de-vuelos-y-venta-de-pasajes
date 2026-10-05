package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.dto.RegistroUsuarioForm;
import com.aerolinea.gestionvuelos.service.UsuarioRegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistroController {

  private final UsuarioRegistrationService usuarioRegistrationService;

  public RegistroController(UsuarioRegistrationService usuarioRegistrationService) {
    this.usuarioRegistrationService = usuarioRegistrationService;
  }

  @GetMapping("/registro")
  public String showRegistrationForm(Model model) {
    if (!model.containsAttribute("registroUsuarioForm")) {
      model.addAttribute("registroUsuarioForm", new RegistroUsuarioForm());
    }
    return "registro";
  }

  @PostMapping("/registro")
  public String register(
      @Valid @ModelAttribute("registroUsuarioForm") RegistroUsuarioForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      return "registro";
    }
    try {
      usuarioRegistrationService.registerPassenger(form);
      redirectAttributes.addFlashAttribute(
          "success", "Registro exitoso. Revisá tu email para activar la cuenta.");
      return "redirect:/login";
    } catch (IllegalArgumentException e) {
      if ("El documento ya está registrado".equals(e.getMessage())) {
        bindingResult.rejectValue("documento", "duplicate", e.getMessage());
      } else if ("El email ya está registrado".equals(e.getMessage())) {
        bindingResult.rejectValue("email", "duplicate", e.getMessage());
      } else if ("La contraseña debe tener al menos una letra y un número".equals(e.getMessage())) {
        bindingResult.rejectValue("password", "policy", e.getMessage());
      } else {
        model.addAttribute("error", e.getMessage());
      }
      return "registro";
    } catch (IllegalStateException e) {
      model.addAttribute("error", e.getMessage());
      return "registro";
    }
  }

  @GetMapping("/registro/confirmar")
  public String confirmAccount(
      @RequestParam(name = "token", required = false) String token,
      RedirectAttributes redirectAttributes) {
    try {
      usuarioRegistrationService.activateAccount(token);
      redirectAttributes.addFlashAttribute(
          "success", "Cuenta activada correctamente. Ya podés iniciar sesión.");
      return "redirect:/login";
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:/registro";
    }
  }
}
