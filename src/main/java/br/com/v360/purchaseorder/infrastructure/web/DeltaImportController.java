package br.com.v360.purchaseorder.infrastructure.web;

import br.com.v360.purchaseorder.infrastructure.web.dto.ImportResultResponse;
import br.com.v360.purchaseorder.integration.delta.DeltaImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports/delta")
public class DeltaImportController {

    private final DeltaImportService importService;

    public DeltaImportController(DeltaImportService importService) {
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultResponse> importOrders(
            @RequestPart("ordersFile") MultipartFile ordersFile,
            @RequestPart("itemsFile") MultipartFile itemsFile
    ) {
        return ResponseEntity.ok(importService.importOrders(ordersFile, itemsFile));
    }
}
