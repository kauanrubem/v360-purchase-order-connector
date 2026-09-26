package br.com.v360.purchaseorder.domain.repository;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderScanItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<PurchaseOrder> findBySourceAndNumber(ClientSource source, String number);

    @Query(
            value = """
                    select po from PurchaseOrder po
                    where po.firstImportedAt <= :snapshotAt
                      and (:source is null or po.source = :source)
                      and (:vendorTaxId is null or po.vendor.taxId = :vendorTaxId)
                      and (:status is null or po.status = :status)
                      and (:pendingOnly = false or exists (
                          select item.id from PurchaseOrderItem item
                          where item.purchaseOrder = po
                            and item.quantityOrdered > item.quantityReceived
                      ))
                    """,
            countQuery = """
                    select count(po) from PurchaseOrder po
                    where po.firstImportedAt <= :snapshotAt
                      and (:source is null or po.source = :source)
                      and (:vendorTaxId is null or po.vendor.taxId = :vendorTaxId)
                      and (:status is null or po.status = :status)
                      and (:pendingOnly = false or exists (
                          select item.id from PurchaseOrderItem item
                          where item.purchaseOrder = po
                            and item.quantityOrdered > item.quantityReceived
                      ))
                    """
    )
    Page<PurchaseOrder> search(
            @Param("source") ClientSource source,
            @Param("vendorTaxId") String vendorTaxId,
            @Param("status") PurchaseOrderStatus status,
            @Param("pendingOnly") boolean pendingOnly,
            @Param("snapshotAt") Instant snapshotAt,
            Pageable pageable
    );

    @Query("""
            select new br.com.v360.purchaseorder.domain.model.PurchaseOrderScanItem(
                po.id, po.source, po.number, po.createdAt, po.status, po.currency,
                po.vendor.taxId, po.vendor.name,
                case when count(pendingItem.id) > 0 then true else false end
            )
            from PurchaseOrder po
            left join po.items pendingItem
                on pendingItem.quantityOrdered > pendingItem.quantityReceived
            where po.firstImportedAt <= :snapshotAt
              and (:source is null or po.source = :source)
              and (:vendorTaxId is null or po.vendor.taxId = :vendorTaxId)
              and (:status is null or po.status = :status)
              and (:pendingOnly = false or exists (
                  select item.id from PurchaseOrderItem item
                  where item.purchaseOrder = po
                    and item.quantityOrdered > item.quantityReceived
              ))
            group by po.id, po.source, po.number, po.createdAt, po.status, po.currency,
                     po.vendor.taxId, po.vendor.name, po.firstImportedAt
            order by po.firstImportedAt asc, po.id asc
            """)
    List<PurchaseOrderScanItem> findItemsForScan(
            @Param("source") ClientSource source,
            @Param("vendorTaxId") String vendorTaxId,
            @Param("status") PurchaseOrderStatus status,
            @Param("pendingOnly") boolean pendingOnly,
            @Param("snapshotAt") Instant snapshotAt
    );
}
