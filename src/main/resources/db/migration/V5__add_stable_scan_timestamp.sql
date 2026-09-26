ALTER TABLE purchase_orders
    ADD COLUMN first_imported_at TIMESTAMP WITH TIME ZONE;

UPDATE purchase_orders
SET first_imported_at = imported_at;

ALTER TABLE purchase_orders
    ALTER COLUMN first_imported_at SET NOT NULL;

CREATE INDEX idx_purchase_orders_snapshot_order
    ON purchase_orders(first_imported_at, id);
