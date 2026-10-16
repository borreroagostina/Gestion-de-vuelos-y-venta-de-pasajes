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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "payment_orders",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_payment_order_code", columnNames = "order_code"),
      @UniqueConstraint(
          name = "uk_payment_external_transaction",
          columnNames = "external_transaction_id")
    })
public class PaymentOrder {

  private static final int ORDER_CODE_LENGTH = 36;
  private static final int AMOUNT_PRECISION = 12;
  private static final int CURRENCY_LENGTH = 3;
  private static final int STATUS_LENGTH = 30;
  private static final int EXTERNAL_TRANSACTION_ID_LENGTH = 100;
  private static final int CHECKOUT_URL_LENGTH = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_code", nullable = false, length = ORDER_CODE_LENGTH)
  private String orderCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reserva_temporal_id", nullable = false)
  private ReservaTemporal reservaTemporal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(nullable = false, precision = AMOUNT_PRECISION, scale = 2)
  private BigDecimal subtotal;

  @Column(name = "tax_amount", nullable = false, precision = AMOUNT_PRECISION, scale = 2)
  private BigDecimal taxAmount;

  @Column(nullable = false, precision = AMOUNT_PRECISION, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = CURRENCY_LENGTH)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = STATUS_LENGTH)
  private PaymentOrderStatus status;

  @Column(name = "external_transaction_id", length = EXTERNAL_TRANSACTION_ID_LENGTH, unique = true)
  private String externalTransactionId;

  @Column(name = "preference_id", length = EXTERNAL_TRANSACTION_ID_LENGTH)
  private String preferenceId;

  @Column(name = "checkout_url", length = CHECKOUT_URL_LENGTH)
  private String checkoutUrl;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public String getOrderCode() {
    return orderCode;
  }

  public void setOrderCode(String orderCode) {
    this.orderCode = orderCode;
  }

  public ReservaTemporal getReservaTemporal() {
    return reservaTemporal;
  }

  public void setReservaTemporal(ReservaTemporal reservaTemporal) {
    this.reservaTemporal = reservaTemporal;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(BigDecimal subtotal) {
    this.subtotal = subtotal;
  }

  public BigDecimal getTaxAmount() {
    return taxAmount;
  }

  public void setTaxAmount(BigDecimal taxAmount) {
    this.taxAmount = taxAmount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public PaymentOrderStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentOrderStatus status) {
    this.status = status;
  }

  public String getExternalTransactionId() {
    return externalTransactionId;
  }

  public void setExternalTransactionId(String externalTransactionId) {
    this.externalTransactionId = externalTransactionId;
  }

  public String getPreferenceId() {
    return preferenceId;
  }

  public void setPreferenceId(String preferenceId) {
    this.preferenceId = preferenceId;
  }

  public String getCheckoutUrl() {
    return checkoutUrl;
  }

  public void setCheckoutUrl(String checkoutUrl) {
    this.checkoutUrl = checkoutUrl;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
