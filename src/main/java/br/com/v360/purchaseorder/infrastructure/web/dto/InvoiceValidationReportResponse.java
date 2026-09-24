package br.com.v360.purchaseorder.infrastructure.web.dto;

import br.com.v360.purchaseorder.domain.model.DivergenceType;

import java.util.Map;

public record InvoiceValidationReportResponse(
        long total,
        long approved,
        long rejected,
        Map<DivergenceType, Long> divergencesByType
) {
}

