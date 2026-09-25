package br.com.v360.purchaseorder.integration.alfa;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderItem;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.domain.model.Vendor;
import br.com.v360.purchaseorder.integration.common.StandardPurchaseOrderFieldMapper;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AlfaPurchaseOrderMapper {

    private final Clock clock;
    private final StandardPurchaseOrderFieldMapper fieldMapper;

    public AlfaPurchaseOrderMapper(Clock clock, StandardPurchaseOrderFieldMapper fieldMapper) {
        this.clock = clock;
        this.fieldMapper = fieldMapper;
    }

    public PurchaseOrder toNewPurchaseOrder(AlfaPurchaseOrderPayload.AlfaOrder source) {
        validate(source);
        return new PurchaseOrder(
                ClientSource.ALFA,
                source.number().trim(),
                source.createdAt(),
                mapStatus(source.status()),
                mapCurrency(source.currency()),
                mapVendor(source.vendor()),
                mapItems(source.items()),
                clock.instant()
        );
    }

    public void update(PurchaseOrder target, AlfaPurchaseOrderPayload.AlfaOrder source) {
        validate(source);
        target.replaceData(
                source.createdAt(),
                mapStatus(source.status()),
                mapCurrency(source.currency()),
                mapVendor(source.vendor()),
                mapItems(source.items()),
                clock.instant()
        );
    }

    private Vendor mapVendor(AlfaPurchaseOrderPayload.AlfaVendor source) {
        return fieldMapper.mapVendor(source.taxId(), source.name());
    }

    private List<PurchaseOrderItem> mapItems(List<AlfaPurchaseOrderPayload.AlfaItem> source) {
        return source.stream()
                .map(item -> fieldMapper.mapItem(
                        item.line().toString(),
                        item.materialCode(),
                        item.description(),
                        item.unitOfMeasure(),
                        item.quantityOrdered(),
                        item.quantityReceived(),
                        item.unitPrice(),
                        null
                ))
                .toList();
    }

    private PurchaseOrderStatus mapStatus(String status) {
        return fieldMapper.mapEnglishStatus(status, "Alfa");
    }

    private String mapCurrency(String currency) {
        return fieldMapper.mapCurrency(currency);
    }

    private void validate(AlfaPurchaseOrderPayload.AlfaOrder source) {
        Set<Integer> lines = new HashSet<>();
        for (AlfaPurchaseOrderPayload.AlfaItem item : source.items()) {
            if (!lines.add(item.line())) {
                throw new InvalidPurchaseOrderException("O pedido possui linhas duplicadas: " + item.line());
            }
            if (item.quantityReceived().compareTo(item.quantityOrdered()) > 0) {
                throw new InvalidPurchaseOrderException(
                        "A quantidade recebida é maior que a pedida na linha " + item.line() + "."
                );
            }
        }
    }
}
