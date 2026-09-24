package br.com.v360.purchaseorder.integration.gama;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderItem;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.domain.model.Vendor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class GamaPurchaseOrderMapper {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final String GAMA_CURRENCY = "BRL";
    private static final String NORMALIZED_UNIT = "UN";

    private final Clock clock;

    public GamaPurchaseOrderMapper(Clock clock) {
        this.clock = clock;
    }

    public PurchaseOrder toNewPurchaseOrder(List<GamaPurchaseOrderPayload> rows) {
        GamaPurchaseOrderPayload header = validateAndGetHeader(rows);
        return new PurchaseOrder(
                ClientSource.GAMA,
                header.orderNumber().trim(),
                Instant.ofEpochSecond(header.createdAtEpochSeconds()).atZone(ZoneOffset.UTC).toLocalDate(),
                mapStatus(header.status()),
                GAMA_CURRENCY,
                mapVendor(header),
                mapItems(rows),
                clock.instant()
        );
    }

    public void update(PurchaseOrder target, List<GamaPurchaseOrderPayload> rows) {
        GamaPurchaseOrderPayload header = validateAndGetHeader(rows);
        target.replaceData(
                Instant.ofEpochSecond(header.createdAtEpochSeconds()).atZone(ZoneOffset.UTC).toLocalDate(),
                mapStatus(header.status()),
                GAMA_CURRENCY,
                mapVendor(header),
                mapItems(rows),
                clock.instant()
        );
    }

    private GamaPurchaseOrderPayload validateAndGetHeader(List<GamaPurchaseOrderPayload> rows) {
        if (rows.isEmpty()) {
            throw new InvalidPurchaseOrderException("O pedido Gama não possui itens.");
        }
        GamaPurchaseOrderPayload header = rows.getFirst();
        for (GamaPurchaseOrderPayload row : rows) {
            boolean consistent = header.orderNumber().equals(row.orderNumber())
                    && header.vendorTaxId().equals(row.vendorTaxId())
                    && header.vendorName().equals(row.vendorName())
                    && header.createdAtEpochSeconds().equals(row.createdAtEpochSeconds())
                    && header.status().equals(row.status());
            if (!consistent) {
                throw new InvalidPurchaseOrderException(
                        "As linhas do pedido Gama %s possuem dados de cabeçalho inconsistentes."
                                .formatted(header.orderNumber())
                );
            }
        }
        return header;
    }

    private Vendor mapVendor(GamaPurchaseOrderPayload source) {
        String taxId = source.vendorTaxId().replaceAll("\\D", "");
        if (taxId.length() != 14) {
            throw new InvalidPurchaseOrderException("O CNPJ do fornecedor deve possuir 14 dígitos.");
        }
        return new Vendor(taxId, source.vendorName().trim());
    }

    private List<PurchaseOrderItem> mapItems(List<GamaPurchaseOrderPayload> rows) {
        Set<Integer> lines = new HashSet<>();
        return rows.stream().map(row -> {
            if (!lines.add(row.item())) {
                throw new InvalidPurchaseOrderException(
                        "O pedido Gama %s possui a linha duplicada %d."
                                .formatted(row.orderNumber(), row.item())
                );
            }
            if (row.quantityReceived().compareTo(row.quantityOrdered()) > 0) {
                throw new InvalidPurchaseOrderException(
                        "A quantidade recebida é maior que a pedida na linha %d do pedido Gama %s."
                                .formatted(row.item(), row.orderNumber())
                );
            }

            BigDecimal purchaseUnitPrice = BigDecimal.valueOf(row.purchaseUnitPriceCents(), 2);
            BigDecimal normalizedUnitPrice = purchaseUnitPrice
                    .divide(row.conversionFactor(), 6, RoundingMode.HALF_UP);

            return new PurchaseOrderItem(
                    row.item().toString(),
                    row.materialCode().trim(),
                    row.description().trim(),
                    NORMALIZED_UNIT,
                    row.quantityOrdered().multiply(row.conversionFactor()),
                    row.quantityReceived().multiply(row.conversionFactor()),
                    normalizedUnitPrice,
                    row.purchaseUnit().trim().toUpperCase(Locale.ROOT),
                    row.conversionFactor(),
                    purchaseUnitPrice
            );
        }).toList();
    }

    private PurchaseOrderStatus mapStatus(int status) {
        return switch (status) {
            case 1 -> PurchaseOrderStatus.OPEN;
            case 2 -> PurchaseOrderStatus.CLOSED;
            case 3 -> PurchaseOrderStatus.BLOCKED;
            default -> throw new InvalidPurchaseOrderException("Situação Gama desconhecida: " + status);
        };
    }
}

