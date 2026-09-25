package br.com.v360.purchaseorder.integration.delta;

import br.com.v360.purchaseorder.domain.repository.InvoiceValidationRepository;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class DeltaImportFlowTest {

    @Autowired
    private MockMvc mockMvc;

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
    void importsHeadersReportsOrphanAndKeepsHeaderWithoutItems() throws Exception {
        importSample()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(4))
                .andExpect(jsonPath("$.imported").value(3))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.errors[0].purchaseOrderNumber").value("DL-2026-0099"))
                .andExpect(jsonPath("$.errors[0].code").value("ORPHAN_ITEM"));

        mockMvc.perform(get("/api/v1/purchase-orders/DELTA/DL-2026-0044"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[1].sourceDetails.createdAt").value("2026-09-08"));

        mockMvc.perform(get("/api/v1/purchase-orders/DELTA/DL-2026-0046"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPendingItems").value(false))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void reimportUpdatesWithoutDuplicatingDeltaOrders() throws Exception {
        importSample().andExpect(status().isOk());
        importSample().andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/purchase-orders").param("source", "DELTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void combinesDeltaFiltersAndOnlyReturnsOrdersWithPendingItems() throws Exception {
        importSample().andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "DELTA")
                        .param("vendorTaxId", "67.890.123/0001-45")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "DELTA")
                        .param("pendingOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].number").value("DL-2026-0044"));
    }

    @Test
    void mapsAllDeltaStatusesToTheSharedVocabulary() throws Exception {
        String orders = """
                {"orders":[
                  {"po_number":"DL-OPEN","created_at":"2026-09-01","status":"open","currency":"BRL",
                   "vendor":{"tax_id":"67890123000145","name":"Fornecedor Delta"}},
                  {"po_number":"DL-CLOSED","created_at":"2026-09-01","status":"closed","currency":"BRL",
                   "vendor":{"tax_id":"67890123000145","name":"Fornecedor Delta"}},
                  {"po_number":"DL-BLOCKED","created_at":"2026-09-01","status":"blocked","currency":"BRL",
                   "vendor":{"tax_id":"67890123000145","name":"Fornecedor Delta"}}
                ]}
                """;
        importPayload(orders, "{\"items\":[]}").andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/purchase-orders/DELTA/DL-OPEN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"));
        mockMvc.perform(get("/api/v1/purchase-orders/DELTA/DL-CLOSED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        mockMvc.perform(get("/api/v1/purchase-orders/DELTA/DL-BLOCKED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void reimportReplacesTheSnapshotWithUpdatedReceivedQuantity() throws Exception {
        importSample().andExpect(status().isOk());
        String updatedItems = validItems().replace("\"quantity_received\":300", "\"quantity_received\":500");
        importPayload(validOrders(), updatedItems).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/purchase-orders/DELTA/DL-2026-0044"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantityReceived").value(500.0))
                .andExpect(jsonPath("$.items[0].quantityRemaining").value(300.0));
    }

    @Test
    void validatesInvoicesUsingTheNormalizedDeltaOrder() throws Exception {
        importSample().andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/purchase-orders/DELTA/DL-2026-0044/invoice-validations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoice("67890123000145", "EMB-500", 500, "1875.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.divergences.length()").value(0));

        mockMvc.perform(post("/api/v1/purchase-orders/DELTA/DL-2026-0044/invoice-validations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoice("11111111000111", "EMB-500", 600, "1.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.divergences.length()").value(3))
                .andExpect(jsonPath("$.divergences[0].type").value("VENDOR_MISMATCH"))
                .andExpect(jsonPath("$.divergences[1].type").value("QUANTITY_EXCEEDS_REMAINING"))
                .andExpect(jsonPath("$.divergences[2].type").value("PRICE_MISMATCH"));
    }

    @Test
    void rejectsInvoiceForClosedDeltaOrder() throws Exception {
        importSample().andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/purchase-orders/DELTA/DL-2026-0045/invoice-validations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoice("78901234000156", "PAP-100", 1, "24.90")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.divergences[0].type").value("PURCHASE_ORDER_CLOSED"));
    }

    @Test
    void rollsBackAllHeadersWhenAJoinedItemViolatesDomainRules() throws Exception {
        String invalidItems = validItems().replace("\"quantity_received\":300", "\"quantity_received\":900");

        importPayload(validOrders(), invalidItems)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Pedido inválido"));

        mockMvc.perform(get("/api/v1/purchase-orders").param("source", "DELTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void rejectsMalformedJsonBeforePersistingAnything() throws Exception {
        MockMultipartFile invalidOrders = new MockMultipartFile(
                "ordersFile", "orders.json", "application/json", "{".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile items = file("itemsFile", "items.json", validItems());

        mockMvc.perform(multipart("/api/v1/imports/delta").file(invalidOrders).file(items))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Pedido inválido"));

        mockMvc.perform(get("/api/v1/purchase-orders").param("source", "DELTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    private org.springframework.test.web.servlet.ResultActions importSample() throws Exception {
        return importPayload(validOrders(), validItems());
    }

    private org.springframework.test.web.servlet.ResultActions importPayload(
            String orders,
            String items
    ) throws Exception {
        return mockMvc.perform(multipart("/api/v1/imports/delta")
                .file(file("ordersFile", "orders.json", orders))
                .file(file("itemsFile", "items.json", items)));
    }

    private MockMultipartFile file(String part, String name, String content) {
        return new MockMultipartFile(
                part,
                name,
                "application/json",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String validOrders() {
        return """
                {"orders":[
                  {"po_number":"DL-2026-0044","created_at":"2026-09-02","status":"open","currency":"BRL",
                   "vendor":{"tax_id":"67890123000145","name":"Embalagens Norte Sul Ltda"}},
                  {"po_number":"DL-2026-0045","created_at":"2026-08-28","status":"closed","currency":"BRL",
                   "vendor":{"tax_id":"78901234000156","name":"Papelaria Central S.A."}},
                  {"po_number":"DL-2026-0046","created_at":"2026-09-10","status":"open","currency":"BRL",
                   "vendor":{"tax_id":"67890123000145","name":"Embalagens Norte Sul Ltda"}}
                ]}
                """;
    }

    private String validItems() {
        return """
                {"items":[
                  {"purchase_order":"DL-2026-0044","created_at":"2026-09-02","line":10,"material":"EMB-500",
                   "description":"Caixa papelão 40x30","uom":"UN","quantity_ordered":800,"quantity_received":300,"unit_price":3.75},
                  {"purchase_order":"DL-2026-0044","created_at":"2026-09-08","line":20,"material":"EMB-720",
                   "description":"Fita adesiva 48mm","uom":"UN","quantity_ordered":240,"quantity_received":0,"unit_price":8.2},
                  {"purchase_order":"DL-2026-0045","created_at":"2026-08-28","line":10,"material":"PAP-100",
                   "description":"Papel A4","uom":"UN","quantity_ordered":150,"quantity_received":150,"unit_price":24.9},
                  {"purchase_order":"DL-2026-0099","created_at":"2026-09-15","line":10,"material":"EMB-500",
                   "description":"Caixa papelão 40x30","uom":"UN","quantity_ordered":100,"quantity_received":0,"unit_price":3.75}
                ]}
                """;
    }

    private String invoice(String taxId, String material, int quantity, String totalAmount) {
        return """
                {
                  "vendorTaxId": "%s",
                  "items": [{
                    "materialCode": "%s",
                    "quantity": %d,
                    "totalAmount": %s
                  }]
                }
                """.formatted(taxId, material, quantity, totalAmount);
    }
}
