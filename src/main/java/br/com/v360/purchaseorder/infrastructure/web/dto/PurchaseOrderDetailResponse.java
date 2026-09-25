package br.com.v360.purchaseorder.infrastructure.web.dto;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderDetailResponse(
        UUID id,
        ClientSource source,
        String number,
        LocalDate createdAt,
        PurchaseOrderStatus status,
        String currency,
        VendorResponse vendor,
        boolean hasPendingItems,
        List<ItemResponse> items,
        Instant importedAt
) {
    public static PurchaseOrderDetailResponse from(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderDetailResponse(
                purchaseOrder.getId(),
                purchaseOrder.getSource(),
                purchaseOrder.getNumber(),
                purchaseOrder.getCreatedAt(),
                purchaseOrder.getStatus(),
                purchaseOrder.getCurrency(),
                VendorResponse.from(purchaseOrder.getVendor()),
                purchaseOrder.hasPendingItems(),
                purchaseOrder.getItems().stream().map(ItemResponse::from).toList(),
                purchaseOrder.getImportedAt()
        );
    }

    public record ItemResponse(
            String line,
            String materialCode,
            String description,
            String unitOfMeasure,
            BigDecimal quantityOrdered,
            BigDecimal quantityReceived,
            BigDecimal quantityRemaining,
            BigDecimal unitPrice,
            SourceItemDetailsResponse sourceDetails
    ) {
        static ItemResponse from(br.com.v360.purchaseorder.domain.model.PurchaseOrderItem item) {
            return new ItemResponse(
                    item.getLine(),
                    item.getMaterialCode(),
                    item.getDescription(),
                    item.getUnitOfMeasure(),
                    item.getQuantityOrdered(),
                    item.getQuantityReceived(),
                    item.getQuantityRemaining(),
                    item.getUnitPrice(),
                    SourceItemDetailsResponse.from(item)
            );
        }
    }

    public record SourceItemDetailsResponse(
            String purchaseUnit,
            BigDecimal conversionFactor,
            BigDecimal purchaseUnitPrice,
            LocalDate createdAt
    ) {
        static SourceItemDetailsResponse from(br.com.v360.purchaseorder.domain.model.PurchaseOrderItem item) {
            if (item.getSourcePurchaseUnit() == null && item.getSourceCreatedAt() == null) {
                return null;
            }
            return new SourceItemDetailsResponse(
                    item.getSourcePurchaseUnit(),
                    item.getSourceConversionFactor(),
                    item.getSourcePurchaseUnitPrice(),
                    item.getSourceCreatedAt()
            );
        }
    }
}
