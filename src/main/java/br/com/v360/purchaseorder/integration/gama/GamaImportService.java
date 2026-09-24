package br.com.v360.purchaseorder.integration.gama;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import br.com.v360.purchaseorder.infrastructure.web.dto.ImportResultResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GamaImportService {

    private final PurchaseOrderRepository repository;
    private final GamaPurchaseOrderMapper mapper;

    public GamaImportService(PurchaseOrderRepository repository, GamaPurchaseOrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public ImportResultResponse importOrders(List<GamaPurchaseOrderPayload> payload) {
        Map<String, List<GamaPurchaseOrderPayload>> rowsByOrder = payload.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        row -> row.orderNumber().trim(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        for (Map.Entry<String, List<GamaPurchaseOrderPayload>> entry : rowsByOrder.entrySet()) {
            PurchaseOrder purchaseOrder = repository
                    .findBySourceAndNumber(ClientSource.GAMA, entry.getKey())
                    .map(existing -> {
                        mapper.update(existing, entry.getValue());
                        return existing;
                    })
                    .orElseGet(() -> mapper.toNewPurchaseOrder(entry.getValue()));
            repository.save(purchaseOrder);
        }

        return ImportResultResponse.success(rowsByOrder.size());
    }
}

