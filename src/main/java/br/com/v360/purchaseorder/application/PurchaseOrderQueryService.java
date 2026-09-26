package br.com.v360.purchaseorder.application;

import br.com.v360.purchaseorder.domain.exception.InvalidRequestException;
import br.com.v360.purchaseorder.domain.exception.ResourceNotFoundException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderScan;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderScanItem;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderScanEntryRepository;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderScanRepository;
import br.com.v360.purchaseorder.infrastructure.web.dto.PageResponse;
import br.com.v360.purchaseorder.infrastructure.web.dto.PurchaseOrderDetailResponse;
import br.com.v360.purchaseorder.infrastructure.web.dto.PurchaseOrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PurchaseOrderQueryService {

    private final PurchaseOrderRepository repository;
    private final PurchaseOrderScanRepository scanRepository;
    private final PurchaseOrderScanEntryRepository scanEntryRepository;
    private final Clock clock;

    public PurchaseOrderQueryService(
            PurchaseOrderRepository repository,
            PurchaseOrderScanRepository scanRepository,
            PurchaseOrderScanEntryRepository scanEntryRepository,
            Clock clock
    ) {
        this.repository = repository;
        this.scanRepository = scanRepository;
        this.scanEntryRepository = scanEntryRepository;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public PageResponse<PurchaseOrderSummaryResponse> search(
            ClientSource source,
            String vendorTaxId,
            PurchaseOrderStatus status,
            boolean pendingOnly,
            UUID snapshotId,
            Pageable pageable
    ) {
        String normalizedTaxId = vendorTaxId == null ? null : digitsOnly(vendorTaxId);
        if (snapshotId == null && pageable.getPageNumber() > 0) {
            throw new InvalidRequestException("Informe o snapshotId retornado na primeira página.");
        }
        PurchaseOrderScan scan = snapshotId == null
                ? createScan(source, normalizedTaxId, status, pendingOnly)
                : loadScan(snapshotId, source, normalizedTaxId, status, pendingOnly);
        Page<PurchaseOrderSummaryResponse> page = scanEntryRepository
                .findByScanIdOrderByPositionAsc(scan.getId(), pageable)
                .map(PurchaseOrderSummaryResponse::from);
        return PageResponse.from(page, scan.getCreatedAt(), scan.getId());
    }

    private PurchaseOrderScan createScan(
            ClientSource source,
            String vendorTaxId,
            PurchaseOrderStatus status,
            boolean pendingOnly
    ) {
        Instant createdAt = clock.instant();
        scanRepository.deleteByCreatedAtBefore(createdAt.minus(Duration.ofHours(24)));
        List<PurchaseOrderScanItem> orders = repository.findItemsForScan(
                source, vendorTaxId, status, pendingOnly, createdAt
        );
        return scanRepository.save(new PurchaseOrderScan(
                createdAt, source, vendorTaxId, status, pendingOnly, orders
        ));
    }

    private PurchaseOrderScan loadScan(
            UUID snapshotId,
            ClientSource source,
            String vendorTaxId,
            PurchaseOrderStatus status,
            boolean pendingOnly
    ) {
        PurchaseOrderScan scan = scanRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot de pedidos não encontrado ou expirado."));
        if (scan.getCreatedAt().isBefore(clock.instant().minus(Duration.ofHours(24)))) {
            throw new ResourceNotFoundException("Snapshot de pedidos não encontrado ou expirado.");
        }
        if (!scan.hasFilters(source, vendorTaxId, status, pendingOnly)) {
            throw new InvalidRequestException("Os filtros devem ser os mesmos utilizados para criar o snapshot.");
        }
        return scan;
    }

    public PurchaseOrderDetailResponse findOne(ClientSource source, String number) {
        PurchaseOrder purchaseOrder = repository.findBySourceAndNumber(source, number)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido %s/%s não encontrado.".formatted(source, number)
                ));
        return PurchaseOrderDetailResponse.from(purchaseOrder);
    }

    private String digitsOnly(String value) {
        return value.replaceAll("\\D", "");
    }
}
