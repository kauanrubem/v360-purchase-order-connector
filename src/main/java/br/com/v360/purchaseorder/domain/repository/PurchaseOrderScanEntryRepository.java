package br.com.v360.purchaseorder.domain.repository;

import br.com.v360.purchaseorder.domain.model.PurchaseOrderScanEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PurchaseOrderScanEntryRepository extends JpaRepository<PurchaseOrderScanEntry, UUID> {
    Page<PurchaseOrderScanEntry> findByScanIdOrderByPositionAsc(UUID scanId, Pageable pageable);
}
