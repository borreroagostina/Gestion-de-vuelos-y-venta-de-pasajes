package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.dto.OcupacionVueloDto;
import com.aerolinea.gestionvuelos.service.OcupacionReportService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/reportes/ocupacion")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOccupancyReportController {

  private final OcupacionReportService reportService;

  public AdminOccupancyReportController(OcupacionReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping
  public String mostrarReporte(
      @RequestParam(required = false) Long vueloId,
      @RequestParam(required = false) String clase,
      @RequestParam(required = false) LocalDate fechaDesde,
      @RequestParam(required = false) LocalDate fechaHasta,
      Model model) {
    cargarFiltros(model, vueloId, clase, fechaDesde, fechaHasta);
    try {
      model.addAttribute(
          "reporte", reportService.generarReporte(vueloId, clase, fechaDesde, fechaHasta));
    } catch (IllegalArgumentException exception) {
      model.addAttribute("reporte", List.of());
      model.addAttribute("error", exception.getMessage());
    }
    return "admin/reporte-ocupacion";
  }

  @GetMapping("/exportar")
  public ResponseEntity<byte[]> exportarReporte(
      @RequestParam(required = false) Long vueloId,
      @RequestParam(required = false) String clase,
      @RequestParam(required = false) LocalDate fechaDesde,
      @RequestParam(required = false) LocalDate fechaHasta) {
    final List<OcupacionVueloDto> reporte;
    try {
      reporte = reportService.generarReporte(vueloId, clase, fechaDesde, fechaHasta);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }

    final StringBuilder csv =
        new StringBuilder(
            "vuelo,fecha_salida,origen,destino,clase,vendidos,capacidad,ocupacion_porcentaje\n");
    for (OcupacionVueloDto fila : reporte) {
      csv.append(campoCsv(fila.codigoVuelo()))
          .append(',')
          .append(campoCsv(fila.fechaSalida().toString()))
          .append(',')
          .append(campoCsv(fila.origen()))
          .append(',')
          .append(campoCsv(fila.destino()))
          .append(',')
          .append(campoCsv(fila.clase()))
          .append(',')
          .append(fila.vendidos())
          .append(',')
          .append(fila.capacidad())
          .append(',')
          .append(fila.porcentajeOcupacion().toPlainString())
          .append('\n');
    }

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
    headers.setContentDisposition(
        ContentDisposition.attachment().filename("reporte-ocupacion.csv").build());
    return ResponseEntity.ok()
        .headers(headers)
        .body(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private void cargarFiltros(
      Model model, Long vueloId, String clase, LocalDate fechaDesde, LocalDate fechaHasta) {
    model.addAttribute("vuelos", reportService.listarVuelos());
    model.addAttribute("vueloId", vueloId);
    model.addAttribute(
        "clase",
        clase == null || clase.isBlank()
            ? OcupacionReportService.TODAS_LAS_CLASES
            : clase.trim().toUpperCase(Locale.ROOT));
    model.addAttribute("fechaDesde", fechaDesde);
    model.addAttribute("fechaHasta", fechaHasta);
  }

  private String campoCsv(String valor) {
    return "\"" + valor.replace("\"", "\"\"") + "\"";
  }
}
