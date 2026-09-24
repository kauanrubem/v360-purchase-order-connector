package br.com.v360.purchaseorder.application;

import br.com.v360.purchaseorder.domain.exception.ResourceNotFoundException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import br.com.v360.purchaseorder.infrastructure.web.dto.PurchaseOrderDetailResponse;
import br.com.v360.purchaseorder.infrastructure.web.dto.PurchaseOrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PurchaseOrderQueryService {

    private final PurchaseOrderRepository repository;

    public PurchaseOrderQueryService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    public Page<PurchaseOrderSummaryResponse> search(
            ClientSource source,
            String vendorTaxId,
            PurchaseOrderStatus status,
            boolean pendingOnly,
            Pageable pageable
    ) {
        String normalizedTaxId = vendorTaxId == null ? null : digitsOnly(vendorTaxId);
        return repository.search(source, normalizedTaxId, status, pendingOnly, pageable)
                .map(PurchaseOrderSummaryResponse::from);
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

