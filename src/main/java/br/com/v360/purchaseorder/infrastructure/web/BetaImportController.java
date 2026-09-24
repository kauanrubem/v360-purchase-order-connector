package br.com.v360.purchaseorder.infrastructure.web;

import br.com.v360.purchaseorder.infrastructure.web.dto.ImportResultResponse;
import br.com.v360.purchaseorder.integration.beta.BetaImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports/beta")
public class BetaImportController {

    private final BetaImportService importService;

    public BetaImportController(BetaImportService importService) {
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultResponse> importOrders(
            @RequestPart MultipartFile headersFile,
            @RequestPart MultipartFile itemsFile
    ) {
        return ResponseEntity.ok(importService.importOrders(headersFile, itemsFile));
    }
}

