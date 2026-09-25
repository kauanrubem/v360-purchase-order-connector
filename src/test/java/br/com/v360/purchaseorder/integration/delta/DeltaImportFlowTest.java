package br.com.v360.purchaseorder.integration.delta;

import br.com.v360.purchaseorder.domain.repository.InvoiceValidationRepository;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
        return mockMvc.perform(multipart("/api/v1/imports/delta")
                .file(file("ordersFile", "orders.json", validOrders()))
                .file(file("itemsFile", "items.json", validItems())));
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
}
