package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.Aeropuerto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AeropuertoRepository extends JpaRepository<Aeropuerto, Long> {
  List<Aeropuerto> findAllByOrderByNombreAsc();

  Page<Aeropuerto> findAllByOrderByNombreAsc(Pageable pageable);

  boolean existsByCodigoIataIgnoreCase(String codigoIata);

  boolean existsByCodigoIataIgnoreCaseAndIdNot(String codigoIata, Long id);

  @Query(
      "SELECT a FROM Aeropuerto a WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) "
          + "OR LOWER(a.ciudad) LIKE LOWER(CONCAT('%', :filtro, '%')) ORDER BY a.nombre ASC")
  Page<Aeropuerto> searchByNombreOrCiudad(String filtro, Pageable pageable);
}
