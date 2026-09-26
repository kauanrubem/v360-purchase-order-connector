package br.com.v360.purchaseorder.integration.alfa;

import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import br.com.v360.purchaseorder.domain.repository.InvoiceValidationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AlfaPurchaseOrderFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseOrderRepository repository;

    @Autowired
    private InvoiceValidationRepository validationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        validationRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void importsAndReturnsNormalizedAlfaPurchaseOrder() throws Exception {
        mockMvc.perform(post("/api/v1/imports/alfa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(1))
                .andExpect(jsonPath("$.imported").value(1));

        mockMvc.perform(get("/api/v1/purchase-orders/ALFA/4500001234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("ALFA"))
                .andExpect(jsonPath("$.number").value("4500001234"))
                .andExpect(jsonPath("$.vendor.taxId").value("23456789000101"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantityRemaining").value(40.0));
    }

    @Test
    void reimportUpdatesInsteadOfDuplicatingPurchaseOrder() throws Exception {
        importPayload(validPayload(60));
        importPayload(validPayload(80));

        mockMvc.perform(get("/api/v1/purchase-orders?source=ALFA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].number").value("4500001234"));

        mockMvc.perform(get("/api/v1/purchase-orders/ALFA/4500001234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantityReceived").value(80.0))
                .andExpect(jsonPath("$.items[0].quantityRemaining").value(20.0));
    }

    @Test
    void rejectsReceivedQuantityGreaterThanOrdered() throws Exception {
        mockMvc.perform(post("/api/v1/imports/alfa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Pedido inválido"));
    }

    @Test
    void keepsTheSameScanBoundaryWhileNewOrdersAreImported() throws Exception {
        importPayload(validPayloadFor("4500001234", 60));

        MvcResult firstPage = mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "ALFA")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.hasNext").value(false))
                .andExpect(jsonPath("$.page.hasPrevious").value(false))
                .andReturn();
        JsonNode firstResponse = objectMapper.readTree(firstPage.getResponse().getContentAsString());
        String snapshotId = firstResponse.path("page").path("snapshotId").asText();

        importPayload(validPayloadFor("4500009999", 0));

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "ALFA")
                        .param("snapshotId", snapshotId)
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.snapshotId").value(snapshotId))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].number").value("4500001234"));

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "ALFA")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.hasNext").value(true));
    }

    @Test
    void keepsFilteredPagesStableWhileExistingOrdersAreReimported() throws Exception {
        importPayload(validPayloadFor("4500001234", 10));
        importPayload(validPayloadFor("4500005678", 20));

        MvcResult firstPage = mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "ALFA")
                        .param("status", "OPEN")
                        .param("pendingOnly", "true")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].number").value("4500001234"))
                .andReturn();
        String snapshotId = objectMapper.readTree(firstPage.getResponse().getContentAsString())
                .path("page").path("snapshotId").asText();

        importPayload(validPayloadFor("4500001234", 100, "closed"));
        importPayload(validPayloadFor("4500005678", 100, "closed"));

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "ALFA")
                        .param("status", "OPEN")
                        .param("pendingOnly", "true")
                        .param("snapshotId", snapshotId)
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.hasPrevious").value(true))
                .andExpect(jsonPath("$.content[0].number").value("4500005678"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].hasPendingItems").value(true));
    }

    @Test
    void rejectsChangingFiltersInsideTheSameSnapshot() throws Exception {
        importPayload(validPayloadFor("4500001234", 10));
        MvcResult response = mockMvc.perform(get("/api/v1/purchase-orders").param("source", "ALFA"))
                .andExpect(status().isOk())
                .andReturn();
        String snapshotId = objectMapper.readTree(response.getResponse().getContentAsString())
                .path("page").path("snapshotId").asText();

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "BETA")
                        .param("snapshotId", snapshotId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void requiresSnapshotIdAfterTheFirstPage() throws Exception {
        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    private void importPayload(String payload) throws Exception {
        mockMvc.perform(post("/api/v1/imports/alfa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private String validPayload(int received) {
        return validPayloadFor("4500001234", received);
    }

    private String validPayloadFor(String number, int received) {
        return validPayloadFor(number, received, "open");
    }

    private String validPayloadFor(String number, int received, String status) {
        return """
                {
                  "purchase_orders": [{
                    "po_number": "%s",
                    "created_at": "2026-08-05",
                    "status": "%s",
                    "currency": "BRL",
                    "vendor": {
                      "tax_id": "23.456.789/0001-01",
                      "name": "Metalúrgica São Jorge S.A."
                    },
                    "items": [{
                      "line": 10,
                      "material": "MAT-1001",
                      "description": "Chapa de aço 2mm",
                      "uom": "UN",
                      "quantity_ordered": 100,
                      "quantity_received": %d,
                      "unit_price": 45.90
                    }]
                  }]
                }
                """.formatted(number, status, received);
    }
}
