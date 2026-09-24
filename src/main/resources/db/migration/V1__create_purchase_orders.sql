CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY,
    source VARCHAR(20) NOT NULL,
    order_number VARCHAR(100) NOT NULL,
    created_at DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    vendor_tax_id VARCHAR(14) NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_purchase_order_source_number UNIQUE (source, order_number),
    CONSTRAINT ck_purchase_order_source CHECK (source IN ('ALFA', 'BETA', 'GAMA')),
    CONSTRAINT ck_purchase_order_status CHECK (status IN ('OPEN', 'CLOSED', 'BLOCKED'))
);

CREATE TABLE purchase_order_items (
    id UUID PRIMARY KEY,
    purchase_order_id UUID NOT NULL,
    line_number VARCHAR(50) NOT NULL,
    material_code VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL,
    quantity_ordered NUMERIC(19, 6) NOT NULL,
    quantity_received NUMERIC(19, 6) NOT NULL,
    unit_price NUMERIC(19, 6) NOT NULL,
    CONSTRAINT fk_item_purchase_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    CONSTRAINT uk_item_purchase_order_line UNIQUE (purchase_order_id, line_number),
    CONSTRAINT ck_item_quantities CHECK (
        quantity_ordered >= 0
        AND quantity_received >= 0
        AND quantity_received <= quantity_ordered
    ),
    CONSTRAINT ck_item_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_purchase_orders_vendor_tax_id ON purchase_orders(vendor_tax_id);
CREATE INDEX idx_purchase_orders_status ON purchase_orders(status);
CREATE INDEX idx_purchase_order_items_material_code ON purchase_order_items(material_code);

