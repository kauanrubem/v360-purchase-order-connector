package br.com.v360.purchaseorder.infrastructure.web.dto;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.ValidationStatus;
import br.com.v360.purchaseorder.domain.repository.InvoiceValidationReportEntryProjection;

import java.time.Instant;
import java.util.UUID;

public record InvoiceValidationReportEntryResponse(
        UUID id,
        ClientSource source,
        String purchaseOrderNumber,
        String vendorTaxId,
        ValidationStatus status,
        Instant validatedAt,
        int divergenceCount
) {
    public static InvoiceValidationReportEntryResponse from(InvoiceValidationReportEntryProjection source) {
        return new InvoiceValidationReportEntryResponse(
                source.getId(),
                source.getSource(),
                source.getPurchaseOrderNumber(),
                source.getVendorTaxId(),
                source.getStatus(),
                source.getValidatedAt(),
                source.getDivergenceCount()
        );
    }
}
