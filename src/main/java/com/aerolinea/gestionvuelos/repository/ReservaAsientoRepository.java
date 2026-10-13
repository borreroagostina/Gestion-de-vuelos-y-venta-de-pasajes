package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaAsientoRepository extends JpaRepository<ReservaAsiento, Long> {

    List<ReservaAsiento> findByReservaTemporalId(Long reservaTemporalId);

    @Query(
            "SELECT COUNT(DISTINCT ra.asiento.id) FROM ReservaAsiento ra "
                    + "WHERE ra.asiento.instanciaVuelo.id = :instanciaId "
                    + "AND ra.asiento.clase = :clase "
                    + "AND ra.estado = :estado "
                    + "AND ra.fechaExpiracion > :ahora")
    long countByInstanciaAndClaseAndEstadoActivo(
            @Param("instanciaId") Long instanciaId,
            @Param("clase") String clase,
            @Param("estado") EstadoReservaAsiento estado,
            @Param("ahora") LocalDateTime ahora);
}
