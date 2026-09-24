package br.com.v360.purchaseorder.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "purchase_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_purchase_order_source_number",
                columnNames = {"source", "order_number"}
        )
)
public class PurchaseOrder {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientSource source;

    @Column(name = "order_number", nullable = false, length = 100)
    private String number;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Embedded
    private Vendor vendor;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<PurchaseOrderItem> items = new ArrayList<>();

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected PurchaseOrder() {
    }

    public PurchaseOrder(
            ClientSource source,
            String number,
            LocalDate createdAt,
            PurchaseOrderStatus status,
            String currency,
            Vendor vendor,
            List<PurchaseOrderItem> items,
            Instant importedAt
    ) {
        this.source = source;
        this.number = number;
        replaceData(createdAt, status, currency, vendor, items, importedAt);
    }

    public void replaceData(
            LocalDate createdAt,
            PurchaseOrderStatus status,
            String currency,
            Vendor vendor,
            List<PurchaseOrderItem> newItems,
            Instant importedAt
    ) {
        this.createdAt = createdAt;
        this.status = status;
        this.currency = currency;
        this.vendor = vendor;
        this.importedAt = importedAt;
        Map<String, PurchaseOrderItem> existingByLine = new HashMap<>();
        items.forEach(item -> existingByLine.put(item.getLine(), item));

        Set<String> incomingLines = new HashSet<>();
        for (PurchaseOrderItem incoming : newItems) {
            incomingLines.add(incoming.getLine());
            PurchaseOrderItem existing = existingByLine.get(incoming.getLine());
            if (existing == null) {
                incoming.attachTo(this);
                items.add(incoming);
            } else {
                existing.replaceDataFrom(incoming);
            }
        }

        items.removeIf(item -> !incomingLines.contains(item.getLine()));
    }

    public boolean hasPendingItems() {
        return items.stream().anyMatch(item -> item.getQuantityRemaining().signum() > 0);
    }

    public UUID getId() {
        return id;
    }

    public ClientSource getSource() {
        return source;
    }

    public String getNumber() {
        return number;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public List<PurchaseOrderItem> getItems() {
        return List.copyOf(items);
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
