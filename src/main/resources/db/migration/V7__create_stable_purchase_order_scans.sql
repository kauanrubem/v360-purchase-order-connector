CREATE TABLE purchase_order_scans (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_filter VARCHAR(20),
    vendor_tax_id_filter VARCHAR(14),
    status_filter VARCHAR(20),
    pending_only BOOLEAN NOT NULL,
    CONSTRAINT ck_purchase_order_scan_source
        CHECK (source_filter IS NULL OR source_filter IN ('ALFA', 'BETA', 'GAMA', 'DELTA')),
    CONSTRAINT ck_purchase_order_scan_status
        CHECK (status_filter IS NULL OR status_filter IN ('OPEN', 'CLOSED', 'BLOCKED'))
);

CREATE TABLE purchase_order_scan_entries (
    id UUID PRIMARY KEY,
    scan_id UUID NOT NULL,
    position INTEGER NOT NULL,
    purchase_order_id UUID NOT NULL,
    source VARCHAR(20) NOT NULL,
    order_number VARCHAR(100) NOT NULL,
    order_created_at DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    vendor_tax_id VARCHAR(14) NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    has_pending_items BOOLEAN NOT NULL,
    CONSTRAINT fk_purchase_order_scan_entry
        FOREIGN KEY (scan_id) REFERENCES purchase_order_scans(id) ON DELETE CASCADE,
    CONSTRAINT uk_purchase_order_scan_position UNIQUE (scan_id, position),
    CONSTRAINT uk_purchase_order_scan_order UNIQUE (scan_id, purchase_order_id)
);

CREATE INDEX idx_purchase_order_scan_created_at
    ON purchase_order_scans(created_at);

CREATE INDEX idx_purchase_order_scan_entries_page
    ON purchase_order_scan_entries(scan_id, position);
