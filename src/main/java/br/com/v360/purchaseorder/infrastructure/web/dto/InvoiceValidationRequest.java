package br.com.v360.purchaseorder.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceValidationRequest(
        @NotBlank String vendorTaxId,
        @NotEmpty List<@Valid InvoiceItemRequest> items
) {
    public record InvoiceItemRequest(
            String purchaseOrderLine,
            @NotBlank String materialCode,
            @NotNull BigDecimal quantity,
            @NotNull @DecimalMin("0") BigDecimal totalAmount
    ) {
    }
}

