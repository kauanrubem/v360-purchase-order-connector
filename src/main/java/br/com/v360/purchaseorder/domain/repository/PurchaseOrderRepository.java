package br.com.v360.purchaseorder.domain.repository;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<PurchaseOrder> findBySourceAndNumber(ClientSource source, String number);

    @Query(
            value = """
                    select po from PurchaseOrder po
                    where (:source is null or po.source = :source)
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
                    where (:source is null or po.source = :source)
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
            Pageable pageable
    );
}

