package br.com.v360.purchaseorder.integration.gama;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GamaPurchaseOrderPayload(
        @JsonProperty("ped") @NotBlank String orderNumber,
        @NotNull @Min(1) Integer item,
        @JsonProperty("cnpj_fornecedor") @NotBlank String vendorTaxId,
        @JsonProperty("nome_fornecedor") @NotBlank String vendorName,
        @JsonProperty("dt_criacao") @NotNull @Min(0) Long createdAtEpochSeconds,
        @JsonProperty("cod_mat") @NotBlank String materialCode,
        @JsonProperty("desc_mat") @NotBlank String description,
        @JsonProperty("um") @NotBlank String purchaseUnit,
        @JsonProperty("fator_conv") @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal conversionFactor,
        @JsonProperty("qtd_ped") @NotNull @DecimalMin("0") BigDecimal quantityOrdered,
        @JsonProperty("qtd_rec") @NotNull @DecimalMin("0") BigDecimal quantityReceived,
        @JsonProperty("preco_unit_centavos") @NotNull @Min(0) Long purchaseUnitPriceCents,
        @JsonProperty("situacao") @NotNull Integer status
) {
}

