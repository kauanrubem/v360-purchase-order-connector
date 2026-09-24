package br.com.v360.purchaseorder.integration.alfa;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import br.com.v360.purchaseorder.infrastructure.web.dto.ImportResultResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlfaImportService {

    private final PurchaseOrderRepository repository;
    private final AlfaPurchaseOrderMapper mapper;

    public AlfaImportService(PurchaseOrderRepository repository, AlfaPurchaseOrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public ImportResultResponse importOrders(AlfaPurchaseOrderPayload payload) {
        for (AlfaPurchaseOrderPayload.AlfaOrder source : payload.purchaseOrders()) {
            PurchaseOrder purchaseOrder = repository
                    .findBySourceAndNumber(ClientSource.ALFA, source.number().trim())
                    .map(existing -> {
                        mapper.update(existing, source);
                        return existing;
                    })
                    .orElseGet(() -> mapper.toNewPurchaseOrder(source));
            repository.save(purchaseOrder);
        }

        return ImportResultResponse.success(payload.purchaseOrders().size());
    }
}

