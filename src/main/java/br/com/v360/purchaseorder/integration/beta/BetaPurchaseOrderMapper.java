package br.com.v360.purchaseorder.integration.beta;

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
public class BetaPurchaseOrderMapper {

    private final Clock clock;

    public BetaPurchaseOrderMapper(Clock clock) {
        this.clock = clock;
    }

    public PurchaseOrder toNewPurchaseOrder(
            BetaCsvParser.BetaHeaderRow header,
            List<BetaCsvParser.BetaItemRow> sourceItems
    ) {
        List<PurchaseOrderItem> items = mapAndValidateItems(header.number(), sourceItems);
        return new PurchaseOrder(
                ClientSource.BETA,
                header.number().trim(),
                header.createdAt(),
                mapStatus(header.status()),
                mapCurrency(header.currency()),
                mapVendor(header.vendorTaxId(), header.vendorName()),
                items,
                clock.instant()
        );
    }

    public void update(
            PurchaseOrder target,
            BetaCsvParser.BetaHeaderRow header,
            List<BetaCsvParser.BetaItemRow> sourceItems
    ) {
        target.replaceData(
                header.createdAt(),
                mapStatus(header.status()),
                mapCurrency(header.currency()),
                mapVendor(header.vendorTaxId(), header.vendorName()),
                mapAndValidateItems(header.number(), sourceItems),
                clock.instant()
        );
    }

    private Vendor mapVendor(String sourceTaxId, String name) {
        String taxId = sourceTaxId.replaceAll("\\D", "");
        if (taxId.length() != 14) {
            throw new InvalidPurchaseOrderException("O CNPJ do fornecedor deve possuir 14 dígitos.");
        }
        return new Vendor(taxId, name.trim());
    }

    private List<PurchaseOrderItem> mapAndValidateItems(
            String orderNumber,
            List<BetaCsvParser.BetaItemRow> sourceItems
    ) {
        if (sourceItems.isEmpty()) {
            throw new InvalidPurchaseOrderException("O pedido Beta %s não possui itens.".formatted(orderNumber));
        }

        Set<String> lines = new HashSet<>();
        return sourceItems.stream().map(item -> {
            if (!lines.add(item.line())) {
                throw new InvalidPurchaseOrderException(
                        "O pedido Beta %s possui a linha duplicada %s.".formatted(orderNumber, item.line())
                );
            }
            if (item.quantityOrdered().signum() < 0 || item.quantityReceived().signum() < 0) {
                throw new InvalidPurchaseOrderException(
                        "O pedido Beta %s possui quantidade negativa na linha %s.".formatted(orderNumber, item.line())
                );
            }
            if (item.quantityReceived().compareTo(item.quantityOrdered()) > 0) {
                throw new InvalidPurchaseOrderException(
                        "A quantidade recebida é maior que a pedida na linha %s do pedido Beta %s."
                                .formatted(item.line(), orderNumber)
                );
            }
            if (item.unitPrice().signum() < 0) {
                throw new InvalidPurchaseOrderException(
                        "O pedido Beta %s possui preço negativo na linha %s.".formatted(orderNumber, item.line())
                );
            }
            return new PurchaseOrderItem(
                    item.line(),
                    item.materialCode().trim(),
                    item.description().trim(),
                    item.unitOfMeasure().trim().toUpperCase(Locale.ROOT),
                    item.quantityOrdered(),
                    item.quantityReceived(),
                    item.unitPrice()
            );
        }).toList();
    }

    private PurchaseOrderStatus mapStatus(String status) {
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "EM ABERTO" -> PurchaseOrderStatus.OPEN;
            case "ENCERRADO" -> PurchaseOrderStatus.CLOSED;
            case "BLOQUEADO" -> PurchaseOrderStatus.BLOCKED;
            default -> throw new InvalidPurchaseOrderException("Situação Beta desconhecida: " + status);
        };
    }

    private String mapCurrency(String currency) {
        try {
            return Currency.getInstance(currency.trim().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new InvalidPurchaseOrderException("Moeda inválida: " + currency);
        }
    }
}

