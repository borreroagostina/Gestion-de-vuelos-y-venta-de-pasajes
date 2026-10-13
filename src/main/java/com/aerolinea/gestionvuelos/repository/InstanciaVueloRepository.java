package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.InstanciaVuelo;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InstanciaVueloRepository extends JpaRepository<InstanciaVuelo, Long> {

  List<InstanciaVuelo> findByVueloProgramadoIdAndFechaSalidaGreaterThanEqual(
      Long vueloId, LocalDate fecha);

  List<InstanciaVuelo> findByVueloProgramadoIdOrderByFechaSalidaAsc(Long vueloId);

  @Query(
      "SELECT i FROM InstanciaVuelo i WHERE "
          + "i.vueloProgramado.origen.id = :origenId "
          + "AND i.vueloProgramado.destino.id = :destinoId "
          + "AND i.fechaSalida BETWEEN :fechaInicio AND :fechaFin "
          + "AND i.cancelada = false "
          + "AND (i.asientosDispEconomy > 0 OR i.asientosDispPrimera > 0) "
          + "ORDER BY "
          + "CASE WHEN :orden = 'HORA_ASC' THEN i.horaSalida END ASC, "
          + "CASE WHEN :orden = 'PRECIO_ASC' THEN i.precioEconomy END ASC, "
          + "i.horaSalida ASC")
  Page<InstanciaVuelo> buscarVuelosPublicos(
      @Param("origenId") Long origenId,
      @Param("destinoId") Long destinoId,
      @Param("fechaInicio") LocalDate fechaInicio,
      @Param("fechaFin") LocalDate fechaFin,
      @Param("orden") String orden,
      Pageable pageable);
}
