package br.com.v360.purchaseorder.infrastructure.web;

import br.com.v360.purchaseorder.infrastructure.web.dto.ImportResultResponse;
import br.com.v360.purchaseorder.integration.alfa.AlfaImportService;
import br.com.v360.purchaseorder.integration.alfa.AlfaPurchaseOrderPayload;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/imports/alfa")
public class AlfaImportController {

    private final AlfaImportService importService;

    public AlfaImportController(AlfaImportService importService) {
        this.importService = importService;
    }

    @PostMapping
    public ResponseEntity<ImportResultResponse> importOrders(
            @Valid @RequestBody AlfaPurchaseOrderPayload payload
    ) {
        return ResponseEntity.ok(importService.importOrders(payload));
    }
}

