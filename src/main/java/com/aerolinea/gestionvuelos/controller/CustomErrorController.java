package com.aerolinea.gestionvuelos.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

  @RequestMapping("/error")
  public String handleError(HttpServletRequest request, Model model) {
    Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
    Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");
    String message = (String) request.getAttribute("javax.servlet.error.message");

    model.addAttribute("status", statusCode);
    model.addAttribute("error", request.getAttribute("javax.servlet.error.status_code"));
    model.addAttribute("message", message);
    model.addAttribute("exception", exception);

    if (exception != null) {
      model.addAttribute("trace", exception.getStackTrace());
    }

    return "error";
  }
}
