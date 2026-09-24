package br.com.v360.purchaseorder.integration.beta;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import br.com.v360.purchaseorder.infrastructure.web.dto.ImportResultResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BetaImportService {

    private final BetaCsvParser parser;
    private final BetaPurchaseOrderMapper mapper;
    private final PurchaseOrderRepository repository;

    public BetaImportService(
            BetaCsvParser parser,
            BetaPurchaseOrderMapper mapper,
            PurchaseOrderRepository repository
    ) {
        this.parser = parser;
        this.mapper = mapper;
        this.repository = repository;
    }

    @Transactional
    public ImportResultResponse importOrders(MultipartFile headersFile, MultipartFile itemsFile) {
        List<BetaCsvParser.BetaHeaderRow> headers = parser.parseHeaders(headersFile);
        List<BetaCsvParser.BetaItemRow> items = parser.parseItems(itemsFile);

        Map<String, BetaCsvParser.BetaHeaderRow> headerByNumber = uniqueHeaders(headers);
        Map<String, List<BetaCsvParser.BetaItemRow>> itemsByOrder = items.stream()
                .collect(Collectors.groupingBy(BetaCsvParser.BetaItemRow::orderNumber));

        Set<String> orphanOrderNumbers = new HashSet<>(itemsByOrder.keySet());
        orphanOrderNumbers.removeAll(headerByNumber.keySet());
        if (!orphanOrderNumbers.isEmpty()) {
            throw new InvalidPurchaseOrderException(
                    "Existem itens sem cabeçalho correspondente no CSV Beta: " + orphanOrderNumbers
            );
        }

        for (BetaCsvParser.BetaHeaderRow header : headers) {
            List<BetaCsvParser.BetaItemRow> orderItems = itemsByOrder.getOrDefault(header.number(), List.of());
            PurchaseOrder purchaseOrder = repository
                    .findBySourceAndNumber(ClientSource.BETA, header.number())
                    .map(existing -> {
                        mapper.update(existing, header, orderItems);
                        return existing;
                    })
                    .orElseGet(() -> mapper.toNewPurchaseOrder(header, orderItems));
            repository.save(purchaseOrder);
        }

        return ImportResultResponse.success(headers.size());
    }

    private Map<String, BetaCsvParser.BetaHeaderRow> uniqueHeaders(List<BetaCsvParser.BetaHeaderRow> headers) {
        try {
            return headers.stream().collect(Collectors.toMap(
                    BetaCsvParser.BetaHeaderRow::number,
                    Function.identity()
            ));
        } catch (IllegalStateException exception) {
            throw new InvalidPurchaseOrderException("O CSV Beta possui números de pedido duplicados no cabeçalho.");
        }
    }
}

