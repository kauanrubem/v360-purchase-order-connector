package br.com.v360.purchaseorder.integration.gama;

import br.com.v360.purchaseorder.domain.repository.InvoiceValidationRepository;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class GamaPurchaseOrderFlowTest {

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
    void groupsRowsAndNormalizesTimestampCentsAndBoxesToUnits() throws Exception {
        importSample();

        mockMvc.perform(get("/api/v1/purchase-orders/GAMA/GL-778"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").value("2026-08-15"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].unitOfMeasure").value("UN"))
                .andExpect(jsonPath("$.items[0].quantityOrdered").value(120.0))
                .andExpect(jsonPath("$.items[0].quantityReceived").value(24.0))
                .andExpect(jsonPath("$.items[0].quantityRemaining").value(96.0))
                .andExpect(jsonPath("$.items[0].unitPrice").value(100.0))
                .andExpect(jsonPath("$.items[0].sourceDetails.purchaseUnit").value("CX"))
                .andExpect(jsonPath("$.items[0].sourceDetails.conversionFactor").value(12.0))
                .andExpect(jsonPath("$.items[0].sourceDetails.purchaseUnitPrice").value(1200.0));
    }

    @Test
    void keepsExistingValidationRulesForGamaNormalizedUnits() throws Exception {
        importSample();

        String invoice = """
                {
                  "vendorTaxId": "34567890000112",
                  "items": [{
                    "materialCode": "TRP-01",
                    "quantity": 96,
                    "totalAmount": 9600.00
                  }]
                }
                """;

        mockMvc.perform(post("/api/v1/purchase-orders/GAMA/GL-778/invoice-validations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.divergences", hasSize(0)));
    }

    @Test
    void filtersOutFullyReceivedClosedOrderWhenPendingOnlyIsEnabled() throws Exception {
        importSample();

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "GAMA")
                        .param("pendingOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].number").value("GL-778"));
    }

    private void importSample() throws Exception {
        String payload = java.nio.file.Files.readString(
                java.nio.file.Path.of("samples/gama/purchase-orders.json"),
                StandardCharsets.UTF_8
        );
        mockMvc.perform(post("/api/v1/imports/gama")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(2))
                .andExpect(jsonPath("$.imported").value(2));
    }
}

