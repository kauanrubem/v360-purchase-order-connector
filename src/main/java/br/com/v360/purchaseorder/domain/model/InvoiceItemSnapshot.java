package br.com.v360.purchaseorder.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_validation_items")
public class InvoiceItemSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validation_id", nullable = false)
    private InvoiceValidation validation;

    @Column(name = "item_index", nullable = false)
    private int itemIndex;

    @Column(name = "purchase_order_line", length = 50)
    private String purchaseOrderLine;

    @Column(name = "material_code", nullable = false, length = 100)
    private String materialCode;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    protected InvoiceItemSnapshot() {
    }

    public InvoiceItemSnapshot(
            int itemIndex,
            String purchaseOrderLine,
            String materialCode,
            BigDecimal quantity,
            BigDecimal totalAmount
    ) {
        this.itemIndex = itemIndex;
        this.purchaseOrderLine = purchaseOrderLine;
        this.materialCode = materialCode;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    void attachTo(InvoiceValidation validation) {
        this.validation = validation;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public String getPurchaseOrderLine() {
        return purchaseOrderLine;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}

