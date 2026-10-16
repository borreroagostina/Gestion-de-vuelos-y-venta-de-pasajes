package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.PaymentOrder;
import com.aerolinea.gestionvuelos.model.PaymentOrderStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
  Optional<PaymentOrder> findByOrderCode(String orderCode);

  Optional<PaymentOrder> findByOrderCodeAndUsuarioEmailIgnoreCase(String orderCode, String email);

  List<PaymentOrder> findByStatus(PaymentOrderStatus status);

  List<PaymentOrder> findByStatusAndReservaTemporalFechaExpiracionBefore(
      PaymentOrderStatus status, LocalDateTime now);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT p FROM PaymentOrder p WHERE p.status = :status "
          + "AND p.reservaTemporal.fechaExpiracion < :now")
  List<PaymentOrder> findExpiredByStatusWithLock(
      @Param("status") PaymentOrderStatus status, @Param("now") LocalDateTime now);

  Optional<PaymentOrder> findByExternalTransactionIdAndStatus(
      String externalTransactionId, PaymentOrderStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM PaymentOrder p WHERE p.orderCode = :orderCode")
  Optional<PaymentOrder> findByOrderCodeWithLock(@Param("orderCode") String orderCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM PaymentOrder p WHERE p.preferenceId = :preferenceId")
  Optional<PaymentOrder> findByPreferenceIdWithLock(@Param("preferenceId") String preferenceId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT p FROM PaymentOrder p WHERE p.orderCode = :orderCode "
          + "AND LOWER(p.usuario.email) = LOWER(:email)")
  Optional<PaymentOrder> findOwnedByOrderCodeWithLock(
      @Param("orderCode") String orderCode, @Param("email") String email);
}
