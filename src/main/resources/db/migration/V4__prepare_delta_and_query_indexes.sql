ALTER TABLE purchase_orders
    DROP CONSTRAINT ck_purchase_order_source;

ALTER TABLE purchase_orders
    ADD CONSTRAINT ck_purchase_order_source
        CHECK (source IN ('ALFA', 'BETA', 'GAMA', 'DELTA'));

ALTER TABLE purchase_order_items
    ADD COLUMN source_created_at DATE;

CREATE INDEX idx_purchase_orders_filter_order
    ON purchase_orders(source, status, vendor_tax_id, created_at DESC, order_number, id);

CREATE INDEX idx_purchase_orders_stable_order
    ON purchase_orders(created_at DESC, order_number, source, id);

CREATE INDEX idx_purchase_order_items_pending
    ON purchase_order_items(purchase_order_id, quantity_ordered, quantity_received);
