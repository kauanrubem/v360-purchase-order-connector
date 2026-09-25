package br.com.v360.purchaseorder.integration.delta;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record DeltaOrderPayload(
        @NotNull List<@Valid DeltaOrder> orders
) {
    public record DeltaOrder(
            @JsonProperty("po_number") @NotBlank String number,
            @JsonProperty("created_at") @NotNull LocalDate createdAt,
            @NotBlank String status,
            @NotBlank String currency,
            @NotNull @Valid DeltaVendor vendor
    ) {
    }

    public record DeltaVendor(
            @JsonProperty("tax_id") @NotBlank String taxId,
            @NotBlank String name
    ) {
    }
}
