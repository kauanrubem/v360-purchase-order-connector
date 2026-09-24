package br.com.v360.purchaseorder.infrastructure.web;

import br.com.v360.purchaseorder.infrastructure.web.dto.ImportResultResponse;
import br.com.v360.purchaseorder.integration.gama.GamaImportService;
import br.com.v360.purchaseorder.integration.gama.GamaPurchaseOrderPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/imports/gama")
public class GamaImportController {

    private final GamaImportService importService;

    public GamaImportController(GamaImportService importService) {
        this.importService = importService;
    }

    @PostMapping
    public ResponseEntity<ImportResultResponse> importOrders(
            @NotEmpty @RequestBody List<@Valid GamaPurchaseOrderPayload> payload
    ) {
        return ResponseEntity.ok(importService.importOrders(payload));
    }
}

