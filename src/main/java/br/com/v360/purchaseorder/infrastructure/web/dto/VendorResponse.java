package br.com.v360.purchaseorder.infrastructure.web.dto;

import br.com.v360.purchaseorder.domain.model.Vendor;

public record VendorResponse(String taxId, String name) {

    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(vendor.getTaxId(), vendor.getName());
    }
}

