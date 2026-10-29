package com.aerolinea.gestionvuelos.controller;

import com.aerolinea.gestionvuelos.service.MyTicketsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pasajero/mis-pasajes")
@PreAuthorize("hasRole('PASAJERO')")
public class MyTicketsController {

  private final MyTicketsService myTicketsService;

  public MyTicketsController(MyTicketsService myTicketsService) {
    this.myTicketsService = myTicketsService;
  }

  @GetMapping
  public String verMisPasajes(Authentication authentication, Model model) {
    final String email = authentication.getName();
    model.addAttribute("compras", myTicketsService.obtenerHistorialCompras(email));
    return "pasajero-mis-pasajes";
  }

  @GetMapping("/orden/{codigoOrden}/factura")
  public ResponseEntity<byte[]> descargarFactura(
      @PathVariable String codigoOrden, Authentication authentication) {
    final byte[] pdfContent =
        myTicketsService.obtenerFacturaPdf(codigoOrden, authentication.getName());

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Factura_" + codigoOrden + ".pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdfContent);
  }

  @GetMapping("/pasaje/{reservaAsientoId}/pdf")
  public ResponseEntity<byte[]> descargarPasaje(
      @PathVariable Long reservaAsientoId, Authentication authentication) {
    final byte[] pdfContent =
        myTicketsService.obtenerPasajePdf(reservaAsientoId, authentication.getName());

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=Pasaje_" + reservaAsientoId + ".pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdfContent);
  }
}
