package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.dto.AsientosVendidosPorClase;
import com.aerolinea.gestionvuelos.model.EstadoReservaAsiento;
import com.aerolinea.gestionvuelos.model.ReservaAsiento;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaAsientoRepository extends JpaRepository<ReservaAsiento, Long> {

  List<ReservaAsiento> findByReservaTemporalId(Long reservaTemporalId);

  List<ReservaAsiento> findByReservaTemporalIdOrderByAsientoNumeroAsc(Long reservaTemporalId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT ra FROM ReservaAsiento ra WHERE ra.reservaTemporal.id = :reservaTemporalId "
          + "ORDER BY ra.asiento.numero ASC")
  List<ReservaAsiento> findByReservaTemporalIdWithLock(
      @Param("reservaTemporalId") Long reservaTemporalId);

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

  @Query(
      "SELECT COUNT(ra) FROM ReservaAsiento ra "
          + "WHERE ra.asiento.instanciaVuelo.id = :instanciaId "
          + "AND UPPER(ra.asiento.clase) = :clase "
          + "AND ra.estado = :estado")
  long countByInstanciaAndClaseAndEstado(
      @Param("instanciaId") Long instanciaId,
      @Param("clase") String clase,
      @Param("estado") EstadoReservaAsiento estado);

  @Query(
      "SELECT DISTINCT ra.usuario.email FROM ReservaAsiento ra "
          + "WHERE ra.asiento.instanciaVuelo.id = :instanciaId "
          + "AND ra.estado = :estado")
  List<String> findDistinctEmailsByInstanciaAndEstado(
      @Param("instanciaId") Long instanciaId, @Param("estado") EstadoReservaAsiento estado);

  @Query(
      "SELECT new com.aerolinea.gestionvuelos.dto.AsientosVendidosPorClase("
          + "ra.asiento.instanciaVuelo.id, UPPER(ra.asiento.clase), "
          + "COUNT(DISTINCT ra.asiento.id)) "
          + "FROM ReservaAsiento ra "
          + "WHERE ra.estado = :estado AND ra.asiento.instanciaVuelo.id IN :instanciaIds "
          + "GROUP BY ra.asiento.instanciaVuelo.id, UPPER(ra.asiento.clase)")
  List<AsientosVendidosPorClase> contarVendidosPorInstancias(
      @Param("instanciaIds") List<Long> instanciaIds, @Param("estado") EstadoReservaAsiento estado);
}
