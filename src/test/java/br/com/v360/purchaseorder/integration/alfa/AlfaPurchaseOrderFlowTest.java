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

    private void importPayload(String payload) throws Exception {
        mockMvc.perform(post("/api/v1/imports/alfa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private String validPayload(int received) {
        return """
                {
                  "purchase_orders": [{
                    "po_number": "4500001234",
                    "created_at": "2026-08-05",
                    "status": "open",
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
                """.formatted(received);
    }
}
