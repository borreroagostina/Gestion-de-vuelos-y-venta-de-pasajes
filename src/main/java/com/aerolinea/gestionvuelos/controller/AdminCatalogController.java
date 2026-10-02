package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.model.Aeronave;
import com.aerolinea.gestionvuelos.model.Aeropuerto;
import com.aerolinea.gestionvuelos.service.CatalogService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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

  private final CatalogService catalogService;

  public AdminCatalogController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/catalogo")
  public String mostrarCatalogo(
      Model model,
      @RequestParam(defaultValue = "1") int pageAero,
      @RequestParam(defaultValue = "1") int pageAeronave,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String filtroAeropuerto,
      @RequestParam(required = false) String filtroAeronave) {
    try {
      List<Aeropuerto> todosAeropuertos = catalogService.listarAeropuertos();
      List<Aeronave> todasAeronaves = catalogService.listarAeronaves();

      // Filtrar aeropuertos
      List<Aeropuerto> aeropuertosFiltrados = todosAeropuertos;
      if (filtroAeropuerto != null && !filtroAeropuerto.isBlank()) {
        String filtro = filtroAeropuerto.toLowerCase();
        aeropuertosFiltrados = todosAeropuertos.stream()
            .filter(a -> a.getNombre().toLowerCase().contains(filtro)
                || a.getCiudad().toLowerCase().contains(filtro))
            .collect(Collectors.toList());
      }

      // Filtrar aeronaves
      List<Aeronave> aeronavesFiltradas = todasAeronaves;
      if (filtroAeronave != null && !filtroAeronave.isBlank()) {
        String filtro = filtroAeronave.toLowerCase();
        aeronavesFiltradas = todasAeronaves.stream()
            .filter(a -> a.getModelo().toLowerCase().contains(filtro)
                || a.getFabricante().toLowerCase().contains(filtro))
            .collect(Collectors.toList());
      }

      // Paginar aeropuertos
      int totalPagesAero = (int) Math.ceil((double) aeropuertosFiltrados.size() / size);
      int startAero = (pageAero-1) * size;
      int endAero = Math.min(startAero + size, aeropuertosFiltrados.size());
      List<Aeropuerto> aeropuertosPaginado = new ArrayList<>();
      if (startAero < aeropuertosFiltrados.size()) {
        aeropuertosPaginado = aeropuertosFiltrados.subList(startAero, endAero);
      }

      // Paginar aeronaves
      int totalPagesAeronave = (int) Math.ceil((double) aeronavesFiltradas.size() / size);
      int startAeronave = (pageAeronave-1) * size;
      int endAeronave = Math.min(startAeronave + size, aeronavesFiltradas.size());
      List<Aeronave> aeronavesPaginado = new ArrayList<>();
      if (startAeronave < aeronavesFiltradas.size()) {
        aeronavesPaginado = aeronavesFiltradas.subList(startAeronave, endAeronave);
      }

      model.addAttribute("aeropuertos", aeropuertosPaginado);
      model.addAttribute("aeronaves", aeronavesPaginado);
      model.addAttribute("nuevoAeropuerto", new Aeropuerto());
      model.addAttribute("nuevoAeronave", new Aeronave());
      model.addAttribute("filtroAeropuerto", filtroAeropuerto);
      model.addAttribute("filtroAeronave", filtroAeronave);
      model.addAttribute("pageAero", pageAero);
      model.addAttribute("pageAeronave", pageAeronave);
      model.addAttribute("size", size);
      model.addAttribute("totalPagesAero", totalPagesAero);
      model.addAttribute("totalPagesAeronave", totalPagesAeronave);
    } catch (Exception e) {
      model.addAttribute("error", "Error al cargar el catálogo: " + e.getMessage());
      e.printStackTrace();
    }
    return "admin/catalogo";
  }

  @GetMapping("/aeropuertos/{id}/editar")
  public String editarAeropuerto(@PathVariable Long id, Model model) {
    Aeropuerto aeropuerto = catalogService.obtenerAeropuerto(id);
    if (aeropuerto == null) {
      return "redirect:/admin/catalogo";
    }
    model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
    model.addAttribute("aeronaves", catalogService.listarAeronaves());
    model.addAttribute("nuevoAeropuerto", aeropuerto);
    model.addAttribute("nuevoAeronave", new Aeronave());
    return "admin/catalogo";
  }

  @GetMapping("/aeronaves/{id}/editar")
  public String editarAeronave(@PathVariable Long id, Model model) {
    Aeronave aeronave = catalogService.obtenerAeronave(id);
    if (aeronave == null) {
      return "redirect:/admin/catalogo";
    }
    model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
    model.addAttribute("aeronaves", catalogService.listarAeronaves());
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
      model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
      model.addAttribute("aeronaves", catalogService.listarAeronaves());
      model.addAttribute("nuevoAeronave", new Aeronave());
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
      model.addAttribute("error", e.getMessage());
      model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
      model.addAttribute("aeronaves", catalogService.listarAeronaves());
      model.addAttribute("nuevoAeronave", new Aeronave());
      return "admin/catalogo";
    }
    return "redirect:/admin/catalogo";
  }

  @PostMapping("/aeropuertos/{id}/eliminar")
  public String eliminarAeropuerto(
      @PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      catalogService.eliminarAeropuerto(id);
      redirectAttributes.addFlashAttribute("success", "Aeropuerto eliminado correctamente.");
    } catch (IllegalStateException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al eliminar el aeropuerto: " + e.getMessage());
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
      model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
      model.addAttribute("aeronaves", catalogService.listarAeronaves());
      model.addAttribute("nuevoAeropuerto", new Aeropuerto());
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
      model.addAttribute("error", e.getMessage());
      model.addAttribute("aeropuertos", catalogService.listarAeropuertos());
      model.addAttribute("aeronaves", catalogService.listarAeronaves());
      model.addAttribute("nuevoAeropuerto", new Aeropuerto());
      return "admin/catalogo";
    }
    return "redirect:/admin/catalogo";
  }

  @PostMapping("/aeronaves/{id}/eliminar")
  public String eliminarAeronave(
      @PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      catalogService.eliminarAeronave(id);
      redirectAttributes.addFlashAttribute("success", "Aeronave eliminada correctamente.");
    } catch (IllegalStateException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al eliminar la aeronave: " + e.getMessage());
    }
    return "redirect:/admin/catalogo";
  }
}
