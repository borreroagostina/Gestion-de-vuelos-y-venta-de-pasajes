package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final int MAX_PAGE_SIZE = 100;

  private final CatalogService catalogService;

  public AdminCatalogController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/catalogo")
  public String mostrarCatalogo(
      Model model,
      @RequestParam(defaultValue = "0") int pageAero,
      @RequestParam(defaultValue = "0") int pageAeronave,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String filtroAeropuerto,
      @RequestParam(required = false) String filtroAeronave) {
    cargarListados(model, pageAero, pageAeronave, size, filtroAeropuerto, filtroAeronave);
    model.addAttribute("nuevoAeropuerto", new Aeropuerto());
    model.addAttribute("nuevoAeronave", new Aeronave());
    return "admin/catalogo";
  }

  @GetMapping("/aeropuertos/{id}/editar")
  public String editarAeropuerto(@PathVariable Long id, Model model) {
    final Aeropuerto aeropuerto = catalogService.obtenerAeropuerto(id);
    if (aeropuerto == null) {
      return "redirect:/admin/catalogo";
    }
    cargarListados(model, 0, 0, DEFAULT_PAGE_SIZE, null, null);
    model.addAttribute("nuevoAeropuerto", aeropuerto);
    model.addAttribute("nuevoAeronave", new Aeronave());
    return "admin/catalogo";
  }

  @GetMapping("/aeronaves/{id}/editar")
  public String editarAeronave(@PathVariable Long id, Model model) {
    final Aeronave aeronave = catalogService.obtenerAeronave(id);
    if (aeronave == null) {
      return "redirect:/admin/catalogo";
    }
    cargarListados(model, 0, 0, DEFAULT_PAGE_SIZE, null, null);
    model.addAttribute("nuevoAeropuerto", new Aeropuerto());
    model.addAttribute("nuevoAeronave", aeronave);
    return "admin/catalogo";
  }

  @PostMapping("/aeropuertos")
  public String guardarAeropuerto(
      @Valid @ModelAttribute("nuevoAeropuerto") Aeropuerto aeropuerto,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    if (bindingResult.hasErrors()) {
      prepararFormularioAeropuerto(model);
      return "admin/catalogo";
    }
    try {
      if (aeropuerto.getId() != null) {
        catalogService.actualizarAeropuerto(aeropuerto);
        redirectAttributes.addFlashAttribute("success", "Aeropuerto actualizado correctamente.");
      } else {
        catalogService.guardarAeropuerto(aeropuerto);
        redirectAttributes.addFlashAttribute("success", "Aeropuerto guardado correctamente.");
      }
    } catch (IllegalArgumentException e) {
      bindingResult.rejectValue("codigoIata", "duplicate", e.getMessage());
      prepararFormularioAeropuerto(model);
      return "admin/catalogo";
    } catch (DataIntegrityViolationException e) {
      bindingResult.rejectValue(
          "codigoIata", "duplicate", "Ya existe un aeropuerto con ese código IATA");
      prepararFormularioAeropuerto(model);
      return "admin/catalogo";
    }
    return "redirect:/admin/catalogo";
  }

  @PostMapping("/aeropuertos/{id}/eliminar")
  public String eliminarAeropuerto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      catalogService.eliminarAeropuerto(id);
      redirectAttributes.addFlashAttribute("success", "Aeropuerto eliminado correctamente.");
    } catch (IllegalStateException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute(
          "error", "Error al eliminar el aeropuerto: " + e.getMessage());
    }
    return "redirect:/admin/catalogo";
  }

  @PostMapping("/aeronaves")
  public String guardarAeronave(
      @Valid @ModelAttribute("nuevoAeronave") Aeronave aeronave,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    if (bindingResult.hasErrors()) {
      prepararFormularioAeronave(model);
      return "admin/catalogo";
    }
    try {
      if (aeronave.getId() != null) {
        catalogService.actualizarAeronave(aeronave);
        redirectAttributes.addFlashAttribute("success", "Aeronave actualizada correctamente.");
      } else {
        catalogService.guardarAeronave(aeronave);
        redirectAttributes.addFlashAttribute("success", "Aeronave guardada correctamente.");
      }
    } catch (IllegalArgumentException e) {
      bindingResult.rejectValue("codigo", "duplicate", e.getMessage());
      prepararFormularioAeronave(model);
      return "admin/catalogo";
    } catch (DataIntegrityViolationException e) {
      bindingResult.rejectValue("codigo", "duplicate", "Ya existe una aeronave con ese código");
      prepararFormularioAeronave(model);
      return "admin/catalogo";
    }
    return "redirect:/admin/catalogo";
  }

  @PostMapping("/aeronaves/{id}/eliminar")
  public String eliminarAeronave(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      catalogService.eliminarAeronave(id);
      redirectAttributes.addFlashAttribute("success", "Aeronave eliminada correctamente.");
    } catch (IllegalStateException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute(
          "error", "Error al eliminar la aeronave: " + e.getMessage());
    }
    return "redirect:/admin/catalogo";
  }

  private void prepararFormularioAeropuerto(Model model) {
    cargarListados(model, 0, 0, DEFAULT_PAGE_SIZE, null, null);
    model.addAttribute("nuevoAeronave", new Aeronave());
  }

  private void prepararFormularioAeronave(Model model) {
    cargarListados(model, 0, 0, DEFAULT_PAGE_SIZE, null, null);
    model.addAttribute("nuevoAeropuerto", new Aeropuerto());
  }

  private void cargarListados(
      Model model,
      int pageAero,
      int pageAeronave,
      int size,
      String filtroAeropuerto,
      String filtroAeronave) {
    final int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    Page<Aeropuerto> aeropuertos =
        catalogService.listarAeropuertosPaginado(Math.max(0, pageAero), pageSize, filtroAeropuerto);
    Page<Aeronave> aeronaves =
        catalogService.listarAeronavesPaginado(Math.max(0, pageAeronave), pageSize, filtroAeronave);

    if (aeropuertos.getTotalPages() > 0 && pageAero >= aeropuertos.getTotalPages()) {
      aeropuertos =
          catalogService.listarAeropuertosPaginado(
              aeropuertos.getTotalPages() - 1, pageSize, filtroAeropuerto);
    }
    if (aeronaves.getTotalPages() > 0 && pageAeronave >= aeronaves.getTotalPages()) {
      aeronaves =
          catalogService.listarAeronavesPaginado(
              aeronaves.getTotalPages() - 1, pageSize, filtroAeronave);
    }

    model.addAttribute("aeropuertos", aeropuertos.getContent());
    model.addAttribute("aeronaves", aeronaves.getContent());
    model.addAttribute("filtroAeropuerto", filtroAeropuerto);
    model.addAttribute("filtroAeronave", filtroAeronave);
    model.addAttribute("pageAero", aeropuertos.getNumber());
    model.addAttribute("pageAeronave", aeronaves.getNumber());
    model.addAttribute("size", pageSize);
    model.addAttribute("totalPagesAero", aeropuertos.getTotalPages());
    model.addAttribute("totalPagesAeronave", aeronaves.getTotalPages());
  }
}
