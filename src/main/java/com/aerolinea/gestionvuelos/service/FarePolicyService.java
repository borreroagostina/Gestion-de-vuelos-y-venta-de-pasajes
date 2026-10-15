package com.aerolinea.gestionvuelos.service;

import com.aerolinea.gestionvuelos.model.FarePolicy;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FarePolicyService {

  public List<FarePolicy> obtenerPoliticas() {
    return List.of(
        new FarePolicy(
            "ECONOMY",
            "1 bolso de mano hasta 8 kg y 45 cm x 35 cm x 20 cm.",
            "1 valija en bodega de 15 kg incluida.",
            "Cambios permitidos con tarifa sujeta a diferencia de precio y cargo por modificación.",
            "Tarifa básica con franquicia de equipaje estándar para viajes de cabina económica."),
        new FarePolicy(
            "PRIMERA",
            "1 bolso de mano hasta 12 kg y 55 cm x 40 cm x 20 cm, más 1 bolso personal.",
            "1 valija en bodega de 23 kg incluida.",
            "Cambios flexibles sin cargo en la mayoría de reservas y reembolso preferencial según"
                + " disponibilidad.",
            "Tarifa premium con mayor flexibilidad, prioridad y equipaje superior."));
  }
}
