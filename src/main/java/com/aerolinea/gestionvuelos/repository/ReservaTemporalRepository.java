package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaTemporalRepository extends JpaRepository<ReservaTemporal, Long> {
  List<ReservaTemporal> findByFechaExpiracionBefore(LocalDateTime now);
}
