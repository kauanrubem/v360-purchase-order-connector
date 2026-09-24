package br.com.v360.purchaseorder.infrastructure.web.dto;

import java.util.List;

public record ImportResultResponse(
        int received,
        int imported,
        int rejected,
        List<ImportErrorResponse> errors
) {
    public static ImportResultResponse success(int count) {
        return new ImportResultResponse(count, count, 0, List.of());
    }

    public record ImportErrorResponse(
            String purchaseOrderNumber,
            String code,
            String message,
            String location
    ) {
    }
}

