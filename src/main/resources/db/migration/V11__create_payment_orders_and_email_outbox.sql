CREATE TABLE payment_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_code VARCHAR(36) NOT NULL,
    reserva_temporal_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    tax_amount DECIMAL(12, 2) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    external_transaction_id VARCHAR(100) NULL,
    preference_id VARCHAR(100) NULL,
    checkout_url VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order_code (order_code),
    UNIQUE KEY uk_payment_external_transaction (external_transaction_id),
    CONSTRAINT fk_payment_order_reservation FOREIGN KEY (reserva_temporal_id)
        REFERENCES reservas_temporales(id),
    CONSTRAINT fk_payment_order_user FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id),
    INDEX idx_payment_order_status_created (status, created_at)
);

CREATE TABLE purchase_email_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    sent_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_email_order (payment_order_id),
    CONSTRAINT fk_purchase_email_order FOREIGN KEY (payment_order_id)
        REFERENCES payment_orders(id),
    INDEX idx_purchase_email_due (status, next_attempt_at)
);
