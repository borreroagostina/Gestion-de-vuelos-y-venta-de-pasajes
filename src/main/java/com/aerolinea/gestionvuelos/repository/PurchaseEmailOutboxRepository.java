package com.aerolinea.gestionvuelos.repository;

import com.aerolinea.gestionvuelos.model.EmailOutboxStatus;
import com.aerolinea.gestionvuelos.model.PurchaseEmailOutbox;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseEmailOutboxRepository extends JpaRepository<PurchaseEmailOutbox, Long> {
  List<PurchaseEmailOutbox> findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
      List<EmailOutboxStatus> statuses, LocalDateTime now);
}
