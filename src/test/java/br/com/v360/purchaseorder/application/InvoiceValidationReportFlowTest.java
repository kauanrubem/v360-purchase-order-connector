package br.com.v360.purchaseorder.application;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class InvoiceValidationReportFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private InvoiceValidationRepository validationRepository;

    @BeforeEach
    void setUp() throws Exception {
        validationRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        importAlfaOrder();
        createValidation("23456789000101", 40, "1836.00");
        createValidation("11111111000111", 50, "1.00");
    }

    @Test
    void consolidatesApprovalsRejectionsAndDivergenceOccurrences() throws Exception {
        mockMvc.perform(get("/api/v1/reports/invoice-validations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.approved").value(1))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.divergencesByType.VENDOR_MISMATCH").value(1))
                .andExpect(jsonPath("$.divergencesByType.QUANTITY_EXCEEDS_REMAINING").value(1))
                .andExpect(jsonPath("$.divergencesByType.PRICE_MISMATCH").value(1));
    }

    @Test
    void filtersReportByClientAndPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/reports/invoice-validations")
                        .param("source", "ALFA")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-12-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(get("/api/v1/reports/invoice-validations")
                        .param("source", "BETA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.approved").value(0))
                .andExpect(jsonPath("$.rejected").value(0));
    }

    @Test
    void rejectsInvertedPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/reports/invoice-validations")
                        .param("from", "2026-12-31T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    private void createValidation(String taxId, int quantity, String totalAmount) throws Exception {
        String request = """
                {
                  "vendorTaxId": "%s",
                  "items": [{
                    "materialCode": "MAT-1001",
                    "quantity": %d,
                    "totalAmount": %s
                  }]
                }
                """.formatted(taxId, quantity, totalAmount);

        mockMvc.perform(post("/api/v1/purchase-orders/ALFA/4500001234/invoice-validations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }

    private void importAlfaOrder() throws Exception {
        String payload = """
                {
                  "purchase_orders": [{
                    "po_number": "4500001234",
                    "created_at": "2026-08-05",
                    "status": "open",
                    "currency": "BRL",
                    "vendor": {"tax_id": "23456789000101", "name": "Metalúrgica São Jorge S.A."},
                    "items": [{
                      "line": 10,
                      "material": "MAT-1001",
                      "description": "Chapa de aço 2mm",
                      "uom": "UN",
                      "quantity_ordered": 100,
                      "quantity_received": 60,
                      "unit_price": 45.90
                    }]
                  }]
                }
                """;

        mockMvc.perform(post("/api/v1/imports/alfa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}

