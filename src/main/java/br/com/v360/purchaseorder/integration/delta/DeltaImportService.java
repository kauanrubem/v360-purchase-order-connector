package br.com.v360.purchaseorder.integration.delta;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import br.com.v360.purchaseorder.infrastructure.web.dto.ImportResultResponse;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class DeltaImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeltaImportService.class);

    private final DeltaJsonParser parser;
    private final DeltaPurchaseOrderAssembler assembler;
    private final DeltaPurchaseOrderMapper mapper;
    private final PurchaseOrderRepository repository;

    public DeltaImportService(
            DeltaJsonParser parser,
            DeltaPurchaseOrderAssembler assembler,
            DeltaPurchaseOrderMapper mapper,
            PurchaseOrderRepository repository
    ) {
        this.parser = parser;
        this.assembler = assembler;
        this.mapper = mapper;
        this.repository = repository;
    }

    @Transactional
    public ImportResultResponse importOrders(MultipartFile ordersFile, MultipartFile itemsFile) {
        DeltaOrderPayload orders = parser.parseOrders(ordersFile);
        DeltaItemPayload items = parser.parseItems(itemsFile);
        DeltaPurchaseOrderAssembler.AssemblyResult assembly = assembler.assemble(orders, items);
        LOGGER.info(
                "Starting purchase order import source=DELTA orders={} orphanItems={}",
                assembly.orders().size(), assembly.orphanItems().size()
        );

        for (DeltaPurchaseOrderAssembler.JoinedOrder source : assembly.orders()) {
            String orderNumber = source.header().number().trim();
            PurchaseOrder purchaseOrder = repository
                    .findBySourceAndNumber(ClientSource.DELTA, orderNumber)
                    .map(existing -> {
                        mapper.update(existing, source);
                        return existing;
                    })
                    .orElseGet(() -> mapper.toNewPurchaseOrder(source));
            repository.save(purchaseOrder);
        }

        List<ImportResultResponse.ImportErrorResponse> errors = assembly.orphanItems().stream()
                .map(this::orphanError)
                .toList();
        int imported = assembly.orders().size();
        int rejected = assembly.orphanItems().size();
        LOGGER.info(
                "Purchase order import completed source=DELTA imported={} rejected={}",
                imported, rejected
        );
        return new ImportResultResponse(imported + rejected, imported, rejected, errors);
    }

    private ImportResultResponse.ImportErrorResponse orphanError(DeltaItemPayload.DeltaItem item) {
        String orderNumber = item.purchaseOrderNumber().trim();
        return new ImportResultResponse.ImportErrorResponse(
                orderNumber,
                "ORPHAN_ITEM",
                "O item aponta para um pedido que não veio no arquivo de cabeçalhos do Delta.",
                "items[purchase_order=%s,line=%d]".formatted(orderNumber, item.line())
        );
    }
}
