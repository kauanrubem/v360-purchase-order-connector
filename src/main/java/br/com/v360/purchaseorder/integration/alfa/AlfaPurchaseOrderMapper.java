package br.com.v360.purchaseorder.integration.alfa;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderItem;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.domain.model.Vendor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AlfaPurchaseOrderMapper {

    private final Clock clock;

    public AlfaPurchaseOrderMapper(Clock clock) {
        this.clock = clock;
    }

    public PurchaseOrder toNewPurchaseOrder(AlfaPurchaseOrderPayload.AlfaOrder source) {
        validate(source);
        return new PurchaseOrder(
                ClientSource.ALFA,
                source.number().trim(),
                source.createdAt(),
                mapStatus(source.status()),
                mapCurrency(source.currency()),
                mapVendor(source.vendor()),
                mapItems(source.items()),
                clock.instant()
        );
    }

    public void update(PurchaseOrder target, AlfaPurchaseOrderPayload.AlfaOrder source) {
        validate(source);
        target.replaceData(
                source.createdAt(),
                mapStatus(source.status()),
                mapCurrency(source.currency()),
                mapVendor(source.vendor()),
                mapItems(source.items()),
                clock.instant()
        );
    }

    private Vendor mapVendor(AlfaPurchaseOrderPayload.AlfaVendor source) {
        String taxId = source.taxId().replaceAll("\\D", "");
        if (taxId.length() != 14) {
            throw new InvalidPurchaseOrderException("O CNPJ do fornecedor deve possuir 14 dígitos.");
        }
        return new Vendor(taxId, source.name().trim());
    }

    private List<PurchaseOrderItem> mapItems(List<AlfaPurchaseOrderPayload.AlfaItem> source) {
        return source.stream()
                .map(item -> new PurchaseOrderItem(
                        item.line().toString(),
                        item.materialCode().trim(),
                        item.description().trim(),
                        item.unitOfMeasure().trim().toUpperCase(Locale.ROOT),
                        item.quantityOrdered(),
                        item.quantityReceived(),
                        item.unitPrice()
                ))
                .toList();
    }

    private PurchaseOrderStatus mapStatus(String status) {
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "open" -> PurchaseOrderStatus.OPEN;
            case "closed" -> PurchaseOrderStatus.CLOSED;
            case "blocked" -> PurchaseOrderStatus.BLOCKED;
            default -> throw new InvalidPurchaseOrderException("Situação Alfa desconhecida: " + status);
        };
    }

    private String mapCurrency(String currency) {
        try {
            return Currency.getInstance(currency.trim().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new InvalidPurchaseOrderException("Moeda inválida: " + currency);
        }
    }

    private void validate(AlfaPurchaseOrderPayload.AlfaOrder source) {
        Set<Integer> lines = new HashSet<>();
        for (AlfaPurchaseOrderPayload.AlfaItem item : source.items()) {
            if (!lines.add(item.line())) {
                throw new InvalidPurchaseOrderException("O pedido possui linhas duplicadas: " + item.line());
            }
            if (item.quantityReceived().compareTo(item.quantityOrdered()) > 0) {
                throw new InvalidPurchaseOrderException(
                        "A quantidade recebida é maior que a pedida na linha " + item.line() + "."
                );
            }
        }
    }
}

