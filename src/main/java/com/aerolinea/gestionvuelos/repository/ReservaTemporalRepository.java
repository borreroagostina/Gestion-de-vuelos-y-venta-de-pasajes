package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.ReservaTemporal;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaTemporalRepository extends JpaRepository<ReservaTemporal, Long> {
  List<ReservaTemporal> findByFechaExpiracionBefore(LocalDateTime now);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM ReservaTemporal r WHERE r.id = :id")
  Optional<ReservaTemporal> findByIdWithLock(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM ReservaTemporal r WHERE r.fechaExpiracion < :now")
  List<ReservaTemporal> findExpiredWithLock(@Param("now") LocalDateTime now);

  Optional<ReservaTemporal> findFirstByUsuarioIdAndSesionIdAndFechaExpiracionAfterOrderByIdDesc(
      Long usuarioId, String sesionId, LocalDateTime now);

  Optional<ReservaTemporal> findFirstByUsuarioIdAndFechaExpiracionAfterOrderByIdDesc(
      Long usuarioId, LocalDateTime now);

  Optional<ReservaTemporal>
      findFirstByUsuarioIdAndSesionIdAndFechaExpiracionAfterOrderByFechaExpiracionDesc(
          Long usuarioId, String sesionId, LocalDateTime now);

  Optional<ReservaTemporal> findFirstByUsuarioIdAndFechaExpiracionAfterOrderByFechaExpiracionDesc(
      Long usuarioId, LocalDateTime now);
}
