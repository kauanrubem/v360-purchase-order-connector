package br.com.v360.purchaseorder.integration.beta;

import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import br.com.v360.purchaseorder.domain.repository.InvoiceValidationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class BetaPurchaseOrderFlowTest {

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
    void importsBothCsvFilesUsingBrazilianFormats() throws Exception {
        mockMvc.perform(multipart("/api/v1/imports/beta")
                        .file(headersFile())
                        .file(itemsFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(2))
                .andExpect(jsonPath("$.imported").value(2));

        mockMvc.perform(get("/api/v1/purchase-orders/BETA/20260088412"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").value("2026-08-15"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.vendor.taxId").value("12345678000190"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].quantityOrdered").value(1200.0))
                .andExpect(jsonPath("$.items[0].quantityReceived").value(400.0))
                .andExpect(jsonPath("$.items[0].unitPrice").value(6.49));
    }

    @Test
    void supportsCombinedNormalizedFilters() throws Exception {
        importValidFiles();

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("source", "BETA")
                        .param("vendorTaxId", "12.345.678/0001-90")
                        .param("status", "OPEN")
                        .param("pendingOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].number").value("20260088412"));
    }

    @Test
    void rejectsItemWithoutMatchingHeader() throws Exception {
        String orphanItems = """
                NUMERO_PEDIDO;ITEM;CODIGO_MATERIAL;DESCRICAO;UNIDADE;QTD_PEDIDA;QTD_RECEBIDA;PRECO_UNITARIO
                999999;1;MAT-1;Material órfão;UN;1,000;0,000;1,00
                """;

        mockMvc.perform(multipart("/api/v1/imports/beta")
                        .file(headersFile())
                        .file(csv("itemsFile", "itens.csv", orphanItems)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Pedido inválido"));
    }

    private void importValidFiles() throws Exception {
        mockMvc.perform(multipart("/api/v1/imports/beta")
                        .file(headersFile())
                        .file(itemsFile()))
                .andExpect(status().isOk());
    }

    private MockMultipartFile headersFile() {
        String content = """
                NUMERO_PEDIDO;FORNECEDOR_CNPJ;FORNECEDOR_RAZAO_SOCIAL;EMISSAO;SITUACAO;MOEDA
                20260088412;12.345.678/0001-90;Distribuidora Horizonte Ltda;15/08/2026;EM ABERTO;BRL
                20260088413;98.765.432/0001-55;Frigorífico Boa Mesa S.A.;01/08/2026;BLOQUEADO;BRL
                """;
        return csv("headersFile", "cabecalho.csv", content);
    }

    private MockMultipartFile itemsFile() {
        String content = """
                NUMERO_PEDIDO;ITEM;CODIGO_MATERIAL;DESCRICAO;UNIDADE;QTD_PEDIDA;QTD_RECEBIDA;PRECO_UNITARIO
                20260088412;1;MAT-77;Óleo de soja 900ml;UN;1.200,000;400,000;6,49
                20260088412;2;MAT-78;Açúcar refinado 1kg;UN;500,000;0,000;4,15
                20260088413;1;MAT-91;Carne bovina dianteiro kg;KG;2.000,000;0,000;27,90
                """;
        return csv("itemsFile", "itens.csv", content);
    }

    private MockMultipartFile csv(String partName, String filename, String content) {
        return new MockMultipartFile(
                partName,
                filename,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
