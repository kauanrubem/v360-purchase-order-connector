CREATE INDEX idx_invoice_validations_report_order
    ON invoice_validations(validated_at DESC, id DESC);

CREATE INDEX idx_invoice_validations_report_filters
    ON invoice_validations(source, status, validated_at DESC, id DESC);
