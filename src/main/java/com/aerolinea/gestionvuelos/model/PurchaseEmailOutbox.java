package com.aerolinea.gestionvuelos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "purchase_email_outbox",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_purchase_email_order", columnNames = "payment_order_id"))
public class PurchaseEmailOutbox {

  private static final int STATUS_LENGTH = 20;
  private static final int MAX_ERROR_LENGTH = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_order_id", nullable = false)
  private PaymentOrder paymentOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = STATUS_LENGTH)
  private EmailOutboxStatus status;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "next_attempt_at", nullable = false)
  private LocalDateTime nextAttemptAt;

  @Column(name = "last_error", length = MAX_ERROR_LENGTH)
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  public Long getId() {
    return id;
  }

  public PaymentOrder getPaymentOrder() {
    return paymentOrder;
  }

  public void setPaymentOrder(PaymentOrder paymentOrder) {
    this.paymentOrder = paymentOrder;
  }

  public EmailOutboxStatus getStatus() {
    return status;
  }

  public void setStatus(EmailOutboxStatus status) {
    this.status = status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public LocalDateTime getNextAttemptAt() {
    return nextAttemptAt;
  }

  public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
    this.nextAttemptAt = nextAttemptAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(LocalDateTime sentAt) {
    this.sentAt = sentAt;
  }
}
