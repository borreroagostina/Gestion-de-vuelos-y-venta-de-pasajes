package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaTemporalRepository extends JpaRepository<ReservaTemporal, Long> {
    List<ReservaTemporal> findByFechaExpiracionBefore(LocalDateTime now);
}
