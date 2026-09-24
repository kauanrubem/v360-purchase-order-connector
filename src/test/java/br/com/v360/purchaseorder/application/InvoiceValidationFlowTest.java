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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class InvoiceValidationFlowTest {

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
    }

    @Test
    void approvesInvoiceMatchingThePurchaseOrder() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders/ALFA/4500001234/invoice-validations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoice("23456789000101", "MAT-1001", 40, "1836.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.divergences", hasSize(0)));

        mockMvc.perform(get("/api/v1/purchase-orders/ALFA/4500001234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantityReceived").value(60.0))
                .andExpect(jsonPath("$.items[0].quantityRemaining").value(40.0));
    }

    @Test
    void returnsAllDetectableDivergences() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders/ALFA/4500001234/invoice-validations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoice("11111111000111", "MAT-1001", 50, "1.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.divergences", hasSize(3)))
                .andExpect(jsonPath("$.divergences[0].type").value("VENDOR_MISMATCH"))
                .andExpect(jsonPath("$.divergences[1].type").value("QUANTITY_EXCEEDS_REMAINING"))
                .andExpect(jsonPath("$.divergences[2].type").value("PRICE_MISMATCH"));
    }

    @Test
    void aggregatesRepeatedInvoiceLinesBeforeCheckingTheBalance() throws Exception {
        String request = """
                {
                  "vendorTaxId": "23456789000101",
                  "items": [
                    {"materialCode": "MAT-1001", "quantity": 25, "totalAmount": 1147.50},
                    {"materialCode": "MAT-1001", "quantity": 20, "totalAmount": 918.00}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/purchase-orders/ALFA/4500001234/invoice-validations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.divergences", hasSize(1)))
                .andExpect(jsonPath("$.divergences[0].type").value("QUANTITY_EXCEEDS_REMAINING"))
                .andExpect(jsonPath("$.divergences[0].actualValue").value("45"));
    }

    @Test
    void recordsPurchaseOrderNotFoundAsBusinessDivergence() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders/BETA/UNKNOWN/invoice-validations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoice("23456789000101", "MAT-1001", 1, "45.90")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.divergences[0].type").value("PURCHASE_ORDER_NOT_FOUND"));
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

    private String invoice(String taxId, String material, int quantity, String total) {
        return """
                {
                  "vendorTaxId": "%s",
                  "items": [{
                    "materialCode": "%s",
                    "quantity": %d,
                    "totalAmount": %s
                  }]
                }
                """.formatted(taxId, material, quantity, total);
    }
}

