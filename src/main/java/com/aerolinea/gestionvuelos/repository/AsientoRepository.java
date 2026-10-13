package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.Asiento;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AsientoRepository extends JpaRepository<Asiento, Long> {

  List<Asiento> findByInstanciaVueloIdOrderByNumeroAsc(Long instanciaVueloId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Asiento a WHERE a.id = :id")
  Optional<Asiento> findByIdWithLock(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Asiento a WHERE a.id IN :ids")
  List<Asiento> findAllByIdWithLock(@Param("ids") Iterable<Long> ids);

  long countByInstanciaVueloIdAndClaseAndEstado(Long instanciaVueloId, String clase, String estado);

  long countByInstanciaVueloIdAndClaseAndEstadoIn(
      Long instanciaVueloId, String clase, Collection<String> estados);
}
