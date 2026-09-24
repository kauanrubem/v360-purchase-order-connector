package br.com.v360.purchaseorder.infrastructure.web.dto;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;

import java.time.LocalDate;
import java.util.UUID;

public record PurchaseOrderSummaryResponse(
        UUID id,
        ClientSource source,
        String number,
        LocalDate createdAt,
        PurchaseOrderStatus status,
        String currency,
        VendorResponse vendor,
        boolean hasPendingItems
) {
    public static PurchaseOrderSummaryResponse from(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderSummaryResponse(
                purchaseOrder.getId(),
                purchaseOrder.getSource(),
                purchaseOrder.getNumber(),
                purchaseOrder.getCreatedAt(),
                purchaseOrder.getStatus(),
                purchaseOrder.getCurrency(),
                VendorResponse.from(purchaseOrder.getVendor()),
                purchaseOrder.hasPendingItems()
        );
    }
}

