ALTER TABLE purchase_order_items
    ADD COLUMN source_purchase_unit VARCHAR(20);

ALTER TABLE purchase_order_items
    ADD COLUMN source_conversion_factor NUMERIC(19, 6);

ALTER TABLE purchase_order_items
    ADD COLUMN source_purchase_unit_price NUMERIC(19, 2);

ALTER TABLE purchase_order_items
    ADD CONSTRAINT ck_source_conversion_factor
        CHECK (source_conversion_factor IS NULL OR source_conversion_factor > 0);
