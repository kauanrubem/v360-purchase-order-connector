package br.com.v360.purchaseorder.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record PurchaseOrderScanItem(
        UUID purchaseOrderId,
        ClientSource source,
        String number,
        LocalDate createdAt,
        PurchaseOrderStatus status,
        String currency,
        String vendorTaxId,
        String vendorName,
        boolean hasPendingItems
) {
}
