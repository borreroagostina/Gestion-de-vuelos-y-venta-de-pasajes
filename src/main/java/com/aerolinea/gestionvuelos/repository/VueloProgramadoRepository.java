package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.VueloProgramado;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VueloProgramadoRepository extends JpaRepository<VueloProgramado, Long> {
  boolean existsByCodigoVueloIgnoreCase(String codigoVuelo);

  boolean existsByCodigoVueloIgnoreCaseAndIdNot(String codigoVuelo, Long id);

  List<VueloProgramado> findAllByOrderByCodigoVueloAsc();

  @Query(
      "SELECT v FROM VueloProgramado v WHERE "
          + "(:origenId IS NULL OR v.origen.id = :origenId) AND "
          + "(:destinoId IS NULL OR v.destino.id = :destinoId) AND "
          + "(:fechaDesde IS NULL OR v.fechaInicioVenta >= :fechaDesde) AND "
          + "(:fechaHasta IS NULL OR v.fechaFinVenta <= :fechaHasta) "
          + "ORDER BY v.codigoVuelo ASC")
  List<VueloProgramado> filtrarVuelos(
      @Param("origenId") Long origenId,
      @Param("destinoId") Long destinoId,
      @Param("fechaDesde") LocalDate fechaDesde,
      @Param("fechaHasta") LocalDate fechaHasta);
}
