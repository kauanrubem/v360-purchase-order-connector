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
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "line_number", nullable = false, length = 50)
    private String line;

    @Column(name = "material_code", nullable = false, length = 100)
    private String materialCode;

    @Column(nullable = false)
    private String description;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "quantity_ordered", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityOrdered;

    @Column(name = "quantity_received", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityReceived;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal unitPrice;

    protected PurchaseOrderItem() {
    }

    public PurchaseOrderItem(
            String line,
            String materialCode,
            String description,
            String unitOfMeasure,
            BigDecimal quantityOrdered,
            BigDecimal quantityReceived,
            BigDecimal unitPrice
    ) {
        this.line = line;
        this.materialCode = materialCode;
        this.description = description;
        this.unitOfMeasure = unitOfMeasure;
        this.quantityOrdered = quantityOrdered;
        this.quantityReceived = quantityReceived;
        this.unitPrice = unitPrice;
    }

    void attachTo(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    void replaceDataFrom(PurchaseOrderItem source) {
        this.materialCode = source.materialCode;
        this.description = source.description;
        this.unitOfMeasure = source.unitOfMeasure;
        this.quantityOrdered = source.quantityOrdered;
        this.quantityReceived = source.quantityReceived;
        this.unitPrice = source.unitPrice;
    }

    public BigDecimal getQuantityRemaining() {
        return quantityOrdered.subtract(quantityReceived);
    }

    public String getLine() {
        return line;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public String getDescription() {
        return description;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public BigDecimal getQuantityOrdered() {
        return quantityOrdered;
    }

    public BigDecimal getQuantityReceived() {
        return quantityReceived;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
