package br.com.v360.purchaseorder.infrastructure.web;

import br.com.v360.purchaseorder.application.PurchaseOrderQueryService;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.infrastructure.web.dto.PageResponse;
import br.com.v360.purchaseorder.infrastructure.web.dto.PurchaseOrderDetailResponse;
import br.com.v360.purchaseorder.infrastructure.web.dto.PurchaseOrderSummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@Validated
@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderQueryService queryService;
    private final Clock clock;

    public PurchaseOrderController(PurchaseOrderQueryService queryService, Clock clock) {
        this.queryService = queryService;
        this.clock = clock;
    }

    @GetMapping
    public PageResponse<PurchaseOrderSummaryResponse> list(
            @RequestParam(required = false) ClientSource source,
            @RequestParam(required = false) String vendorTaxId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(defaultValue = "false") boolean pendingOnly,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant snapshotAt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Instant effectiveSnapshotAt = snapshotAt == null ? clock.instant() : snapshotAt;
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("firstImportedAt"), Sort.Order.asc("id"))
        );
        return PageResponse.from(
                queryService.search(source, vendorTaxId, status, pendingOnly, effectiveSnapshotAt, pageable),
                effectiveSnapshotAt
        );
    }

    @GetMapping("/{source}/{purchaseOrderNumber}")
    public PurchaseOrderDetailResponse detail(
            @PathVariable ClientSource source,
            @PathVariable String purchaseOrderNumber
    ) {
        return queryService.findOne(source, purchaseOrderNumber);
    }
}
