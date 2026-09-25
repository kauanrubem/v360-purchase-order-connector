package br.com.v360.purchaseorder.domain;

import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderItem;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.domain.model.Vendor;
import br.com.v360.purchaseorder.domain.repository.InvoiceValidationRepository;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DeltaPersistenceCompatibilityTest {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private InvoiceValidationRepository validationRepository;

    @BeforeEach
    void cleanDatabase() {
        validationRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
    }

    @Test
    void persistsDeltaSourceAndOptionalItemCreationDate() {
        LocalDate itemCreatedAt = LocalDate.of(2026, 9, 8);
        PurchaseOrderItem item = new PurchaseOrderItem(
                "20",
                "EMB-720",
                "Fita adesiva 48mm",
                "UN",
                new BigDecimal("240"),
                BigDecimal.ZERO,
                new BigDecimal("8.20"),
                null,
                null,
                null,
                itemCreatedAt
        );
        PurchaseOrder order = new PurchaseOrder(
                ClientSource.DELTA,
                "DL-2026-0044",
                LocalDate.of(2026, 9, 2),
                PurchaseOrderStatus.OPEN,
                "BRL",
                new Vendor("67890123000145", "Embalagens Norte Sul Ltda"),
                List.of(item),
                Instant.parse("2026-09-22T12:00:00Z")
        );

        purchaseOrderRepository.saveAndFlush(order);

        PurchaseOrder persisted = purchaseOrderRepository
                .findBySourceAndNumber(ClientSource.DELTA, "DL-2026-0044")
                .orElseThrow();
        assertThat(persisted.getSource()).isEqualTo(ClientSource.DELTA);
        assertThat(persisted.getItems()).singleElement()
                .extracting(PurchaseOrderItem::getSourceCreatedAt)
                .isEqualTo(itemCreatedAt);
    }

    @Test
    void keepsSourceCreationDateOptionalForExistingClients() {
        PurchaseOrderItem item = new PurchaseOrderItem(
                "10",
                "MAT-1001",
                "Chapa de aço 2mm",
                "UN",
                new BigDecimal("100"),
                new BigDecimal("60"),
                new BigDecimal("45.90")
        );
        PurchaseOrder order = new PurchaseOrder(
                ClientSource.ALFA,
                "4500001234",
                LocalDate.of(2026, 8, 5),
                PurchaseOrderStatus.OPEN,
                "BRL",
                new Vendor("23456789000101", "Metalúrgica São Jorge S.A."),
                List.of(item),
                Instant.parse("2026-09-22T12:00:00Z")
        );

        purchaseOrderRepository.saveAndFlush(order);

        PurchaseOrder persisted = purchaseOrderRepository
                .findBySourceAndNumber(ClientSource.ALFA, "4500001234")
                .orElseThrow();
        assertThat(persisted.getItems()).singleElement()
                .extracting(PurchaseOrderItem::getSourceCreatedAt)
                .isNull();
    }
}
