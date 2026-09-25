package br.com.v360.purchaseorder.integration.delta;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.integration.common.StandardPurchaseOrderFieldMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeltaPurchaseOrderAssemblerTest {

    private final DeltaPurchaseOrderAssembler assembler = new DeltaPurchaseOrderAssembler();
    private final DeltaPurchaseOrderMapper mapper = new DeltaPurchaseOrderMapper(
            Clock.fixed(Instant.parse("2026-09-22T12:00:00Z"), ZoneOffset.UTC),
            new StandardPurchaseOrderFieldMapper()
    );

    @Test
    void joinsItemsKeepsEmptyOrdersAndSeparatesOrphans() {
        DeltaOrderPayload orders = new DeltaOrderPayload(List.of(
                order("DL-2026-0044", LocalDate.of(2026, 9, 2)),
                order("DL-2026-0046", LocalDate.of(2026, 9, 10))
        ));
        DeltaItemPayload items = new DeltaItemPayload(List.of(
                item("DL-2026-0044", 10, LocalDate.of(2026, 9, 2)),
                item("DL-2026-0044", 20, LocalDate.of(2026, 9, 8)),
                item("DL-2026-0099", 10, LocalDate.of(2026, 9, 15))
        ));

        DeltaPurchaseOrderAssembler.AssemblyResult result = assembler.assemble(orders, items);

        assertThat(result.orders()).hasSize(2);
        assertThat(result.orders().get(0).items()).hasSize(2);
        assertThat(result.orders().get(1).items()).isEmpty();
        assertThat(result.orphanItems()).singleElement()
                .extracting(DeltaItemPayload.DeltaItem::purchaseOrderNumber)
                .isEqualTo("DL-2026-0099");
    }

    @Test
    void mapsJoinedDataToTheSharedDomainAndPreservesItemDate() {
        DeltaPurchaseOrderAssembler.JoinedOrder source = new DeltaPurchaseOrderAssembler.JoinedOrder(
                order("DL-2026-0044", LocalDate.of(2026, 9, 2)),
                List.of(item("DL-2026-0044", 20, LocalDate.of(2026, 9, 8)))
        );

        PurchaseOrder mapped = mapper.toNewPurchaseOrder(source);

        assertThat(mapped.getSource()).isEqualTo(ClientSource.DELTA);
        assertThat(mapped.getCreatedAt()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(mapped.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getLine()).isEqualTo("20");
            assertThat(item.getSourceCreatedAt()).isEqualTo(LocalDate.of(2026, 9, 8));
        });
    }

    @Test
    void rejectsDuplicateHeadersAndDuplicateLines() {
        DeltaOrderPayload.DeltaOrder header = order("DL-2026-0044", LocalDate.of(2026, 9, 2));
        assertThatThrownBy(() -> assembler.assemble(
                new DeltaOrderPayload(List.of(header, header)),
                new DeltaItemPayload(List.of())
        )).isInstanceOf(InvalidPurchaseOrderException.class)
                .hasMessageContaining("Cabeçalho Delta duplicado");

        DeltaItemPayload.DeltaItem repeated = item("DL-2026-0044", 10, LocalDate.of(2026, 9, 2));
        assertThatThrownBy(() -> assembler.assemble(
                new DeltaOrderPayload(List.of(header)),
                new DeltaItemPayload(List.of(repeated, repeated))
        )).isInstanceOf(InvalidPurchaseOrderException.class)
                .hasMessageContaining("linha duplicada");
    }

    private DeltaOrderPayload.DeltaOrder order(String number, LocalDate createdAt) {
        return new DeltaOrderPayload.DeltaOrder(
                number,
                createdAt,
                "open",
                "BRL",
                new DeltaOrderPayload.DeltaVendor("67.890.123/0001-45", "Embalagens Norte Sul Ltda")
        );
    }

    private DeltaItemPayload.DeltaItem item(String orderNumber, int line, LocalDate createdAt) {
        return new DeltaItemPayload.DeltaItem(
                orderNumber,
                createdAt,
                line,
                "EMB-500",
                "Caixa papelão 40x30",
                "un",
                new BigDecimal("800"),
                new BigDecimal("300"),
                new BigDecimal("3.75")
        );
    }
}
