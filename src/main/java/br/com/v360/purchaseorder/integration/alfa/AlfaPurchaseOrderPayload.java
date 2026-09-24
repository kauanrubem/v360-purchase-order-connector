package br.com.v360.purchaseorder.integration.alfa;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AlfaPurchaseOrderPayload(
        @JsonProperty("purchase_orders")
        @NotEmpty
        List<@Valid AlfaOrder> purchaseOrders
) {
    public record AlfaOrder(
            @JsonProperty("po_number") @NotBlank String number,
            @JsonProperty("created_at") @NotNull LocalDate createdAt,
            @NotBlank String status,
            @NotBlank String currency,
            @NotNull @Valid AlfaVendor vendor,
            @NotEmpty List<@Valid AlfaItem> items
    ) {
    }

    public record AlfaVendor(
            @JsonProperty("tax_id") @NotBlank String taxId,
            @NotBlank String name
    ) {
    }

    public record AlfaItem(
            @NotNull Integer line,
            @JsonProperty("material") @NotBlank String materialCode,
            @NotBlank String description,
            @JsonProperty("uom") @NotBlank String unitOfMeasure,
            @JsonProperty("quantity_ordered") @NotNull @DecimalMin("0") BigDecimal quantityOrdered,
            @JsonProperty("quantity_received") @NotNull @DecimalMin("0") BigDecimal quantityReceived,
            @JsonProperty("unit_price") @NotNull @DecimalMin("0") BigDecimal unitPrice
    ) {
    }
}

