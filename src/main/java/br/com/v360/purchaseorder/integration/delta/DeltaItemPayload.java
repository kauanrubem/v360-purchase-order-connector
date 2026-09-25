package br.com.v360.purchaseorder.integration.delta;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DeltaItemPayload(
        @NotNull List<@Valid DeltaItem> items
) {
    public record DeltaItem(
            @JsonProperty("purchase_order") @NotBlank String purchaseOrderNumber,
            @JsonProperty("created_at") @NotNull LocalDate createdAt,
            @NotNull @Min(1) Integer line,
            @JsonProperty("material") @NotBlank String materialCode,
            @NotBlank String description,
            @JsonProperty("uom") @NotBlank String unitOfMeasure,
            @JsonProperty("quantity_ordered") @NotNull @DecimalMin("0") BigDecimal quantityOrdered,
            @JsonProperty("quantity_received") @NotNull @DecimalMin("0") BigDecimal quantityReceived,
            @JsonProperty("unit_price") @NotNull @DecimalMin("0") BigDecimal unitPrice
    ) {
    }
}
