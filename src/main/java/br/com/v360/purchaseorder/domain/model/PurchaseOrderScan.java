package br.com.v360.purchaseorder.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_scans")
public class PurchaseOrderScan {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_filter", length = 20)
    private ClientSource source;

    @Column(name = "vendor_tax_id_filter", length = 14)
    private String vendorTaxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_filter", length = 20)
    private PurchaseOrderStatus status;

    @Column(name = "pending_only", nullable = false)
    private boolean pendingOnly;

    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private final List<PurchaseOrderScanEntry> entries = new ArrayList<>();

    protected PurchaseOrderScan() {
    }

    public PurchaseOrderScan(
            Instant createdAt,
            ClientSource source,
            String vendorTaxId,
            PurchaseOrderStatus status,
            boolean pendingOnly,
            List<PurchaseOrderScanItem> purchaseOrders
    ) {
        this.createdAt = Objects.requireNonNull(createdAt);
        this.source = source;
        this.vendorTaxId = vendorTaxId;
        this.status = status;
        this.pendingOnly = pendingOnly;
        for (int position = 0; position < purchaseOrders.size(); position++) {
            entries.add(new PurchaseOrderScanEntry(this, position, purchaseOrders.get(position)));
        }
    }

    public boolean hasFilters(
            ClientSource requestedSource,
            String requestedVendorTaxId,
            PurchaseOrderStatus requestedStatus,
            boolean requestedPendingOnly
    ) {
        return source == requestedSource
                && Objects.equals(vendorTaxId, requestedVendorTaxId)
                && status == requestedStatus
                && pendingOnly == requestedPendingOnly;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
