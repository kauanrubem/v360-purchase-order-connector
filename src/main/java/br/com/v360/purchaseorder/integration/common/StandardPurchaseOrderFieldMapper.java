package br.com.v360.purchaseorder.integration.common;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderItem;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.domain.model.Vendor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Locale;

@Component
public class StandardPurchaseOrderFieldMapper {

    public Vendor mapVendor(String sourceTaxId, String name) {
        String taxId = sourceTaxId.replaceAll("\\D", "");
        if (taxId.length() != 14) {
            throw new InvalidPurchaseOrderException("O CNPJ do fornecedor deve possuir 14 dígitos.");
        }
        return new Vendor(taxId, name.trim());
    }

    public PurchaseOrderStatus mapEnglishStatus(String status, String clientName) {
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "open" -> PurchaseOrderStatus.OPEN;
            case "closed" -> PurchaseOrderStatus.CLOSED;
            case "blocked" -> PurchaseOrderStatus.BLOCKED;
            default -> throw new InvalidPurchaseOrderException(
                    "Situação %s desconhecida: %s".formatted(clientName, status)
            );
        };
    }

    public String mapCurrency(String currency) {
        try {
            return Currency.getInstance(currency.trim().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new InvalidPurchaseOrderException("Moeda inválida: " + currency);
        }
    }

    public PurchaseOrderItem mapItem(
            String line,
            String materialCode,
            String description,
            String unitOfMeasure,
            BigDecimal quantityOrdered,
            BigDecimal quantityReceived,
            BigDecimal unitPrice,
            LocalDate sourceCreatedAt
    ) {
        if (quantityOrdered.signum() < 0 || quantityReceived.signum() < 0) {
            throw new InvalidPurchaseOrderException("As quantidades do item não podem ser negativas.");
        }
        if (quantityReceived.compareTo(quantityOrdered) > 0) {
            throw new InvalidPurchaseOrderException(
                    "A quantidade recebida é maior que a pedida na linha " + line + "."
            );
        }
        if (unitPrice.signum() < 0) {
            throw new InvalidPurchaseOrderException("O preço do item não pode ser negativo.");
        }
        return new PurchaseOrderItem(
                line,
                materialCode.trim(),
                description.trim(),
                unitOfMeasure.trim().toUpperCase(Locale.ROOT),
                quantityOrdered,
                quantityReceived,
                unitPrice,
                null,
                null,
                null,
                sourceCreatedAt
        );
    }
}
