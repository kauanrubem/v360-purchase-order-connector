package br.com.v360.purchaseorder.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoice_validations")
public class InvoiceValidation {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientSource source;

    @Column(name = "purchase_order_number", nullable = false, length = 100)
    private String purchaseOrderNumber;

    @Column(name = "vendor_tax_id", nullable = false, length = 14)
    private String vendorTaxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationStatus status;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;

    @OneToMany(mappedBy = "validation", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<InvoiceItemSnapshot> invoiceItems = new ArrayList<>();

    @OneToMany(mappedBy = "validation", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Divergence> divergences = new ArrayList<>();

    protected InvoiceValidation() {
    }

    public InvoiceValidation(
            PurchaseOrder purchaseOrder,
            ClientSource source,
            String purchaseOrderNumber,
            String vendorTaxId,
            List<InvoiceItemSnapshot> invoiceItems,
            List<Divergence> divergences,
            Instant validatedAt
    ) {
        this.purchaseOrder = purchaseOrder;
        this.source = source;
        this.purchaseOrderNumber = purchaseOrderNumber;
        this.vendorTaxId = vendorTaxId;
        this.status = divergences.isEmpty() ? ValidationStatus.APPROVED : ValidationStatus.REJECTED;
        this.validatedAt = validatedAt;
        invoiceItems.forEach(item -> {
            item.attachTo(this);
            this.invoiceItems.add(item);
        });
        divergences.forEach(divergence -> {
            divergence.attachTo(this);
            this.divergences.add(divergence);
        });
    }

    public UUID getId() {
        return id;
    }

    public ClientSource getSource() {
        return source;
    }

    public String getPurchaseOrderNumber() {
        return purchaseOrderNumber;
    }

    public String getVendorTaxId() {
        return vendorTaxId;
    }

    public ValidationStatus getStatus() {
        return status;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public List<InvoiceItemSnapshot> getInvoiceItems() {
        return List.copyOf(invoiceItems);
    }

    public List<Divergence> getDivergences() {
        return List.copyOf(divergences);
    }
}

