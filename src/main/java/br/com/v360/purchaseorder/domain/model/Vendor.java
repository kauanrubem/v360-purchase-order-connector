package br.com.v360.purchaseorder.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Vendor {

    @Column(name = "vendor_tax_id", nullable = false, length = 14)
    private String taxId;

    @Column(name = "vendor_name", nullable = false)
    private String name;

    protected Vendor() {
    }

    public Vendor(String taxId, String name) {
        this.taxId = taxId;
        this.name = name;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getName() {
        return name;
    }
}

