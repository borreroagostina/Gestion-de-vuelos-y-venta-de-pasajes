ALTER TABLE payment_orders
    ADD COLUMN empleado_id BIGINT NULL,
    ADD COLUMN payment_method VARCHAR(20) NULL,
    ADD CONSTRAINT fk_payment_order_employee FOREIGN KEY (empleado_id) REFERENCES usuarios(id);
