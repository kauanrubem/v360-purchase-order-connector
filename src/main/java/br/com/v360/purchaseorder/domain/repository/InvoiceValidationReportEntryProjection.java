package br.com.v360.purchaseorder.domain.repository;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.ValidationStatus;

import java.time.Instant;
import java.util.UUID;

public interface InvoiceValidationReportEntryProjection {

    UUID getId();

    ClientSource getSource();

    String getPurchaseOrderNumber();

    String getVendorTaxId();

    ValidationStatus getStatus();

    Instant getValidatedAt();

    int getDivergenceCount();
}
