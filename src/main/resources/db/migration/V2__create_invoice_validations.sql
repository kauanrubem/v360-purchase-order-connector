CREATE TABLE invoice_validations (
    id UUID PRIMARY KEY,
    purchase_order_id UUID,
    source VARCHAR(20) NOT NULL,
    purchase_order_number VARCHAR(100) NOT NULL,
    vendor_tax_id VARCHAR(14) NOT NULL,
    status VARCHAR(20) NOT NULL,
    validated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_validation_purchase_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
    CONSTRAINT ck_validation_status CHECK (status IN ('APPROVED', 'REJECTED'))
);

CREATE TABLE invoice_validation_items (
    id UUID PRIMARY KEY,
    validation_id UUID NOT NULL,
    item_index INTEGER NOT NULL,
    purchase_order_line VARCHAR(50),
    material_code VARCHAR(100) NOT NULL,
    quantity NUMERIC(19, 6) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_validation_item_validation
        FOREIGN KEY (validation_id) REFERENCES invoice_validations(id) ON DELETE CASCADE
);

CREATE TABLE invoice_validation_divergences (
    id UUID PRIMARY KEY,
    validation_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    invoice_item_index INTEGER,
    purchase_order_line VARCHAR(50),
    material_code VARCHAR(100),
    expected_value VARCHAR(255),
    actual_value VARCHAR(255),
    CONSTRAINT fk_divergence_validation
        FOREIGN KEY (validation_id) REFERENCES invoice_validations(id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_validations_source ON invoice_validations(source);
CREATE INDEX idx_invoice_validations_validated_at ON invoice_validations(validated_at);
CREATE INDEX idx_validation_divergences_type ON invoice_validation_divergences(type);

