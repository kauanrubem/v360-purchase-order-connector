package br.com.v360.purchaseorder.application;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import br.com.v360.purchaseorder.domain.exception.ResourceNotFoundException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.Divergence;
import br.com.v360.purchaseorder.domain.model.DivergenceType;
import br.com.v360.purchaseorder.domain.model.InvoiceItemSnapshot;
import br.com.v360.purchaseorder.domain.model.InvoiceValidation;
import br.com.v360.purchaseorder.domain.model.PurchaseOrder;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderItem;
import br.com.v360.purchaseorder.domain.model.PurchaseOrderStatus;
import br.com.v360.purchaseorder.domain.repository.InvoiceValidationRepository;
import br.com.v360.purchaseorder.domain.repository.PurchaseOrderRepository;
import br.com.v360.purchaseorder.infrastructure.web.dto.InvoiceValidationRequest;
import br.com.v360.purchaseorder.infrastructure.web.dto.InvoiceValidationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceValidationService {

    private static final BigDecimal PRICE_TOLERANCE = new BigDecimal("0.01");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InvoiceValidationRepository validationRepository;
    private final Clock clock;

    public InvoiceValidationService(
            PurchaseOrderRepository purchaseOrderRepository,
            InvoiceValidationRepository validationRepository,
            Clock clock
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.validationRepository = validationRepository;
        this.clock = clock;
    }

    @Transactional
    public InvoiceValidationResponse validate(
            ClientSource source,
            String purchaseOrderNumber,
            InvoiceValidationRequest request
    ) {
        String vendorTaxId = normalizeTaxId(request.vendorTaxId());
        Optional<PurchaseOrder> purchaseOrder = purchaseOrderRepository
                .findBySourceAndNumber(source, purchaseOrderNumber);

        List<InvoiceItemSnapshot> snapshots = snapshots(request);
        List<Divergence> divergences = new ArrayList<>();

        if (purchaseOrder.isEmpty()) {
            divergences.add(divergence(
                    DivergenceType.PURCHASE_ORDER_NOT_FOUND,
                    "O pedido informado não foi encontrado.",
                    null, null, null,
                    source + "/" + purchaseOrderNumber,
                    null
            ));
        } else {
            validatePurchaseOrder(purchaseOrder.get(), vendorTaxId, request, divergences);
        }

        InvoiceValidation validation = new InvoiceValidation(
                purchaseOrder.orElse(null),
                source,
                purchaseOrderNumber,
                vendorTaxId,
                snapshots,
                divergences,
                clock.instant()
        );
        return InvoiceValidationResponse.from(validationRepository.save(validation));
    }

    @Transactional(readOnly = true)
    public InvoiceValidationResponse findById(UUID id) {
        InvoiceValidation validation = validationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conferência %s não encontrada.".formatted(id)));
        return InvoiceValidationResponse.from(validation);
    }

    private void validatePurchaseOrder(
            PurchaseOrder purchaseOrder,
            String vendorTaxId,
            InvoiceValidationRequest request,
            List<Divergence> divergences
    ) {
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CLOSED) {
            divergences.add(divergence(
                    DivergenceType.PURCHASE_ORDER_CLOSED,
                    "O pedido está encerrado.",
                    null, null, null, PurchaseOrderStatus.OPEN.name(), PurchaseOrderStatus.CLOSED.name()
            ));
        } else if (purchaseOrder.getStatus() == PurchaseOrderStatus.BLOCKED) {
            divergences.add(divergence(
                    DivergenceType.PURCHASE_ORDER_BLOCKED,
                    "O pedido está bloqueado.",
                    null, null, null, PurchaseOrderStatus.OPEN.name(), PurchaseOrderStatus.BLOCKED.name()
            ));
        }

        if (!purchaseOrder.getVendor().getTaxId().equals(vendorTaxId)) {
            divergences.add(divergence(
                    DivergenceType.VENDOR_MISMATCH,
                    "O CNPJ do fornecedor não corresponde ao pedido.",
                    null, null, null, purchaseOrder.getVendor().getTaxId(), vendorTaxId
            ));
        }

        Map<String, MatchedTotals> totalsByLine = new LinkedHashMap<>();
        for (int index = 0; index < request.items().size(); index++) {
            InvoiceValidationRequest.InvoiceItemRequest invoiceItem = request.items().get(index);
            if (invoiceItem.quantity().signum() <= 0) {
                divergences.add(divergence(
                        DivergenceType.INVALID_QUANTITY,
                        "A quantidade da nota deve ser maior que zero.",
                        index, invoiceItem.purchaseOrderLine(), invoiceItem.materialCode(), "> 0", invoiceItem.quantity().toPlainString()
                ));
                continue;
            }

            Optional<PurchaseOrderItem> matched = matchItem(purchaseOrder, invoiceItem, index, divergences);
            if (matched.isPresent()) {
                PurchaseOrderItem item = matched.get();
                totalsByLine
                        .computeIfAbsent(item.getLine(), ignored -> new MatchedTotals(item))
                        .add(index, invoiceItem.quantity(), invoiceItem.totalAmount());
            }
        }

        totalsByLine.values().forEach(totals -> validateTotals(totals, divergences));
    }

    private Optional<PurchaseOrderItem> matchItem(
            PurchaseOrder purchaseOrder,
            InvoiceValidationRequest.InvoiceItemRequest invoiceItem,
            int index,
            List<Divergence> divergences
    ) {
        if (invoiceItem.purchaseOrderLine() != null && !invoiceItem.purchaseOrderLine().isBlank()) {
            Optional<PurchaseOrderItem> byLine = purchaseOrder.getItems().stream()
                    .filter(item -> item.getLine().equals(invoiceItem.purchaseOrderLine().trim()))
                    .findFirst();
            if (byLine.isEmpty()) {
                divergences.add(divergence(
                        DivergenceType.PURCHASE_ORDER_ITEM_NOT_FOUND,
                        "A linha informada não existe no pedido.",
                        index, invoiceItem.purchaseOrderLine(), invoiceItem.materialCode(), null, invoiceItem.purchaseOrderLine()
                ));
                return Optional.empty();
            }
            if (!byLine.get().getMaterialCode().equals(invoiceItem.materialCode().trim())) {
                divergences.add(divergence(
                        DivergenceType.MATERIAL_MISMATCH,
                        "O material não corresponde à linha informada.",
                        index,
                        byLine.get().getLine(),
                        invoiceItem.materialCode(),
                        byLine.get().getMaterialCode(),
                        invoiceItem.materialCode()
                ));
                return Optional.empty();
            }
            return byLine;
        }

        List<PurchaseOrderItem> byMaterial = purchaseOrder.getItems().stream()
                .filter(item -> item.getMaterialCode().equals(invoiceItem.materialCode().trim()))
                .toList();
        if (byMaterial.isEmpty()) {
            divergences.add(divergence(
                    DivergenceType.MATERIAL_NOT_FOUND,
                    "O material não existe no pedido.",
                    index, null, invoiceItem.materialCode(), null, invoiceItem.materialCode()
            ));
            return Optional.empty();
        }
        if (byMaterial.size() > 1) {
            divergences.add(divergence(
                    DivergenceType.AMBIGUOUS_MATERIAL,
                    "O material aparece em mais de uma linha; informe a linha do pedido.",
                    index, null, invoiceItem.materialCode(), "uma linha do pedido", null
            ));
            return Optional.empty();
        }
        return Optional.of(byMaterial.getFirst());
    }

    private void validateTotals(MatchedTotals totals, List<Divergence> divergences) {
        BigDecimal remaining = totals.item.getQuantityRemaining();
        if (totals.quantity.compareTo(remaining) > 0) {
            divergences.add(divergence(
                    DivergenceType.QUANTITY_EXCEEDS_REMAINING,
                    "A quantidade total da nota excede o saldo disponível no item do pedido.",
                    totals.firstInvoiceItemIndex,
                    totals.item.getLine(),
                    totals.item.getMaterialCode(),
                    remaining.toPlainString(),
                    totals.quantity.toPlainString()
            ));
        }

        BigDecimal expected = totals.quantity.multiply(totals.item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actual = totals.totalAmount.setScale(2, RoundingMode.HALF_UP);
        if (expected.subtract(actual).abs().compareTo(PRICE_TOLERANCE) > 0) {
            divergences.add(divergence(
                    DivergenceType.PRICE_MISMATCH,
                    "O valor total da nota não corresponde ao preço acordado.",
                    totals.firstInvoiceItemIndex,
                    totals.item.getLine(),
                    totals.item.getMaterialCode(),
                    expected.toPlainString(),
                    actual.toPlainString()
            ));
        }
    }

    private List<InvoiceItemSnapshot> snapshots(InvoiceValidationRequest request) {
        List<InvoiceItemSnapshot> snapshots = new ArrayList<>();
        for (int index = 0; index < request.items().size(); index++) {
            InvoiceValidationRequest.InvoiceItemRequest item = request.items().get(index);
            snapshots.add(new InvoiceItemSnapshot(
                    index,
                    blankToNull(item.purchaseOrderLine()),
                    item.materialCode().trim(),
                    item.quantity(),
                    item.totalAmount()
            ));
        }
        return snapshots;
    }

    private String normalizeTaxId(String value) {
        String normalized = value.replaceAll("\\D", "");
        if (normalized.length() != 14) {
            throw new InvalidPurchaseOrderException("O CNPJ da nota deve possuir 14 dígitos.");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Divergence divergence(
            DivergenceType type,
            String message,
            Integer invoiceItemIndex,
            String purchaseOrderLine,
            String materialCode,
            String expected,
            String actual
    ) {
        return new Divergence(type, message, invoiceItemIndex, purchaseOrderLine, materialCode, expected, actual);
    }

    private static final class MatchedTotals {
        private final PurchaseOrderItem item;
        private int firstInvoiceItemIndex;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        private MatchedTotals(PurchaseOrderItem item) {
            this.item = item;
        }

        private void add(int invoiceItemIndex, BigDecimal addedQuantity, BigDecimal addedTotalAmount) {
            if (quantity.signum() == 0) {
                firstInvoiceItemIndex = invoiceItemIndex;
            }
            quantity = quantity.add(addedQuantity);
            totalAmount = totalAmount.add(addedTotalAmount);
        }
    }
}
