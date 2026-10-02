package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.Aeronave;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AeronaveRepository extends JpaRepository<Aeronave, Long> {
  List<Aeronave> findAllByOrderByModeloAsc();

  Page<Aeronave> findAllByOrderByModeloAsc(Pageable pageable);

  boolean existsByCodigoIgnoreCase(String codigo);

  boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

  @Query(
      "SELECT a FROM Aeronave a WHERE LOWER(a.modelo) LIKE LOWER(CONCAT('%', :filtro, '%')) "
          + "OR LOWER(a.fabricante) LIKE LOWER(CONCAT('%', :filtro, '%')) ORDER BY a.modelo ASC")
  Page<Aeronave> searchByModeloOrFabricante(String filtro, Pageable pageable);
}
