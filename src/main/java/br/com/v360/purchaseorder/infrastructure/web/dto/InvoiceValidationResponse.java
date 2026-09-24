package br.com.v360.purchaseorder.infrastructure.web.dto;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.Divergence;
import br.com.v360.purchaseorder.domain.model.DivergenceType;
import br.com.v360.purchaseorder.domain.model.InvoiceItemSnapshot;
import br.com.v360.purchaseorder.domain.model.InvoiceValidation;
import br.com.v360.purchaseorder.domain.model.ValidationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceValidationResponse(
        UUID id,
        ClientSource source,
        String purchaseOrderNumber,
        String vendorTaxId,
        ValidationStatus status,
        Instant validatedAt,
        List<InvoiceItemResponse> items,
        List<DivergenceResponse> divergences
) {
    public static InvoiceValidationResponse from(InvoiceValidation validation) {
        return new InvoiceValidationResponse(
                validation.getId(),
                validation.getSource(),
                validation.getPurchaseOrderNumber(),
                validation.getVendorTaxId(),
                validation.getStatus(),
                validation.getValidatedAt(),
                validation.getInvoiceItems().stream().map(InvoiceItemResponse::from).toList(),
                validation.getDivergences().stream().map(DivergenceResponse::from).toList()
        );
    }

    public record InvoiceItemResponse(
            int itemIndex,
            String purchaseOrderLine,
            String materialCode,
            BigDecimal quantity,
            BigDecimal totalAmount
    ) {
        static InvoiceItemResponse from(InvoiceItemSnapshot item) {
            return new InvoiceItemResponse(
                    item.getItemIndex(),
                    item.getPurchaseOrderLine(),
                    item.getMaterialCode(),
                    item.getQuantity(),
                    item.getTotalAmount()
            );
        }
    }

    public record DivergenceResponse(
            DivergenceType type,
            String message,
            Integer invoiceItemIndex,
            String purchaseOrderLine,
            String materialCode,
            String expectedValue,
            String actualValue
    ) {
        static DivergenceResponse from(Divergence divergence) {
            return new DivergenceResponse(
                    divergence.getType(),
                    divergence.getMessage(),
                    divergence.getInvoiceItemIndex(),
                    divergence.getPurchaseOrderLine(),
                    divergence.getMaterialCode(),
                    divergence.getExpectedValue(),
                    divergence.getActualValue()
            );
        }
    }
}

