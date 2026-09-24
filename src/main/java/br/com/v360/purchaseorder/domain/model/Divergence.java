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

import java.util.UUID;

@Entity
@Table(name = "invoice_validation_divergences")
public class Divergence {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validation_id", nullable = false)
    private InvoiceValidation validation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DivergenceType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "invoice_item_index")
    private Integer invoiceItemIndex;

    @Column(name = "purchase_order_line", length = 50)
    private String purchaseOrderLine;

    @Column(name = "material_code", length = 100)
    private String materialCode;

    @Column(name = "expected_value", length = 255)
    private String expectedValue;

    @Column(name = "actual_value", length = 255)
    private String actualValue;

    protected Divergence() {
    }

    public Divergence(
            DivergenceType type,
            String message,
            Integer invoiceItemIndex,
            String purchaseOrderLine,
            String materialCode,
            String expectedValue,
            String actualValue
    ) {
        this.type = type;
        this.message = message;
        this.invoiceItemIndex = invoiceItemIndex;
        this.purchaseOrderLine = purchaseOrderLine;
        this.materialCode = materialCode;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    void attachTo(InvoiceValidation validation) {
        this.validation = validation;
    }

    public DivergenceType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Integer getInvoiceItemIndex() {
        return invoiceItemIndex;
    }

    public String getPurchaseOrderLine() {
        return purchaseOrderLine;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }
}

