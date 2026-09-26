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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderQueryService queryService;

    public PurchaseOrderController(PurchaseOrderQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public PageResponse<PurchaseOrderSummaryResponse> list(
            @RequestParam(required = false) ClientSource source,
            @RequestParam(required = false) String vendorTaxId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(defaultValue = "false") boolean pendingOnly,
            @RequestParam(required = false) UUID snapshotId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return queryService.search(
                source, vendorTaxId, status, pendingOnly, snapshotId, PageRequest.of(page, size)
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
