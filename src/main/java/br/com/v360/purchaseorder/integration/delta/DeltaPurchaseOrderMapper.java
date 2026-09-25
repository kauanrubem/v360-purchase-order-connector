package br.com.v360.purchaseorder.integration.delta;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderItem;
import br.com.v360.purchaseorder.integration.common.StandardPurchaseOrderFieldMapper;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
public class DeltaPurchaseOrderMapper {

    private final Clock clock;
    private final StandardPurchaseOrderFieldMapper fieldMapper;

    public DeltaPurchaseOrderMapper(Clock clock, StandardPurchaseOrderFieldMapper fieldMapper) {
        this.clock = clock;
        this.fieldMapper = fieldMapper;
    }

    public PurchaseOrder toNewPurchaseOrder(DeltaPurchaseOrderAssembler.JoinedOrder source) {
        DeltaOrderPayload.DeltaOrder header = source.header();
        return new PurchaseOrder(
                ClientSource.DELTA,
                header.number().trim(),
                header.createdAt(),
                fieldMapper.mapEnglishStatus(header.status(), "Delta"),
                fieldMapper.mapCurrency(header.currency()),
                fieldMapper.mapVendor(header.vendor().taxId(), header.vendor().name()),
                mapItems(source.items()),
                clock.instant()
        );
    }

    public void update(PurchaseOrder target, DeltaPurchaseOrderAssembler.JoinedOrder source) {
        DeltaOrderPayload.DeltaOrder header = source.header();
        target.replaceData(
                header.createdAt(),
                fieldMapper.mapEnglishStatus(header.status(), "Delta"),
                fieldMapper.mapCurrency(header.currency()),
                fieldMapper.mapVendor(header.vendor().taxId(), header.vendor().name()),
                mapItems(source.items()),
                clock.instant()
        );
    }

    private List<PurchaseOrderItem> mapItems(List<DeltaItemPayload.DeltaItem> items) {
        return items.stream()
                .map(item -> fieldMapper.mapItem(
                        item.line().toString(),
                        item.materialCode(),
                        item.description(),
                        item.unitOfMeasure(),
                        item.quantityOrdered(),
                        item.quantityReceived(),
                        item.unitPrice(),
                        item.createdAt()
                ))
                .toList();
    }
}
