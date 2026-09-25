package br.com.v360.purchaseorder.integration.delta;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DeltaPurchaseOrderAssembler {

    public AssemblyResult assemble(DeltaOrderPayload orderPayload, DeltaItemPayload itemPayload) {
        Map<String, DeltaOrderPayload.DeltaOrder> headersByNumber = indexHeaders(orderPayload.orders());
        Map<String, List<DeltaItemPayload.DeltaItem>> itemsByOrder = new LinkedHashMap<>();
        List<DeltaItemPayload.DeltaItem> orphanItems = new ArrayList<>();

        for (DeltaItemPayload.DeltaItem item : itemPayload.items()) {
            String orderNumber = item.purchaseOrderNumber().trim();
            if (!headersByNumber.containsKey(orderNumber)) {
                orphanItems.add(item);
                continue;
            }
            itemsByOrder.computeIfAbsent(orderNumber, ignored -> new ArrayList<>()).add(item);
        }

        List<JoinedOrder> orders = headersByNumber.entrySet().stream()
                .map(entry -> new JoinedOrder(
                        entry.getValue(),
                        List.copyOf(itemsByOrder.getOrDefault(entry.getKey(), List.of()))
                ))
                .toList();
        return new AssemblyResult(orders, List.copyOf(orphanItems));
    }

    private Map<String, DeltaOrderPayload.DeltaOrder> indexHeaders(List<DeltaOrderPayload.DeltaOrder> headers) {
        Map<String, DeltaOrderPayload.DeltaOrder> indexed = new LinkedHashMap<>();
        for (DeltaOrderPayload.DeltaOrder header : headers) {
            String number = header.number().trim();
            if (indexed.putIfAbsent(number, header) != null) {
                throw new InvalidPurchaseOrderException("Cabeçalho Delta duplicado para o pedido " + number + ".");
            }
        }
        return indexed;
    }

    public record JoinedOrder(
            DeltaOrderPayload.DeltaOrder header,
            List<DeltaItemPayload.DeltaItem> items
    ) {
        public JoinedOrder {
            Set<Integer> lines = new HashSet<>();
            for (DeltaItemPayload.DeltaItem item : items) {
                if (!lines.add(item.line())) {
                    throw new InvalidPurchaseOrderException(
                            "O pedido Delta %s possui a linha duplicada %d."
                                    .formatted(header.number(), item.line())
                    );
                }
            }
            items = List.copyOf(items);
        }
    }

    public record AssemblyResult(
            List<JoinedOrder> orders,
            List<DeltaItemPayload.DeltaItem> orphanItems
    ) {
        public AssemblyResult {
            orders = List.copyOf(orders);
            orphanItems = List.copyOf(orphanItems);
        }
    }
}
