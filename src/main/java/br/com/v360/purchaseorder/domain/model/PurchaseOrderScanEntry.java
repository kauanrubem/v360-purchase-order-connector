package br.com.v360.purchaseorder.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_scan_entries")
public class PurchaseOrderScanEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private PurchaseOrderScan scan;

    @Column(nullable = false)
    private int position;

    @Column(name = "purchase_order_id", nullable = false)
    private UUID purchaseOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientSource source;

    @Column(name = "order_number", nullable = false, length = 100)
    private String number;

    @Column(name = "order_created_at", nullable = false)
    private LocalDate createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "vendor_tax_id", nullable = false, length = 14)
    private String vendorTaxId;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "has_pending_items", nullable = false)
    private boolean hasPendingItems;

    protected PurchaseOrderScanEntry() {
    }

    PurchaseOrderScanEntry(PurchaseOrderScan scan, int position, PurchaseOrderScanItem order) {
        this.scan = scan;
        this.position = position;
        this.purchaseOrderId = order.purchaseOrderId();
        this.source = order.source();
        this.number = order.number();
        this.createdAt = order.createdAt();
        this.status = order.status();
        this.currency = order.currency();
        this.vendorTaxId = order.vendorTaxId();
        this.vendorName = order.vendorName();
        this.hasPendingItems = order.hasPendingItems();
    }

    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public ClientSource getSource() { return source; }
    public String getNumber() { return number; }
    public LocalDate getCreatedAt() { return createdAt; }
    public PurchaseOrderStatus getStatus() { return status; }
    public String getCurrency() { return currency; }
    public String getVendorTaxId() { return vendorTaxId; }
    public String getVendorName() { return vendorName; }
    public boolean hasPendingItems() { return hasPendingItems; }
}
