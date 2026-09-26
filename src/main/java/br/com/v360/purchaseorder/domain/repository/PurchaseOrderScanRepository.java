package br.com.v360.purchaseorder.domain.repository;

import br.com.v360.purchaseorder.domain.model.PurchaseOrderScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface PurchaseOrderScanRepository extends JpaRepository<PurchaseOrderScan, UUID> {
    void deleteByCreatedAtBefore(Instant threshold);
}
