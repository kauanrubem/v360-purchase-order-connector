package br.com.v360.purchaseorder.infrastructure.web;

import br.com.v360.purchaseorder.application.InvoiceValidationService;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.infrastructure.web.dto.InvoiceValidationRequest;
import br.com.v360.purchaseorder.infrastructure.web.dto.InvoiceValidationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class InvoiceValidationController {

    private final InvoiceValidationService validationService;

    public InvoiceValidationController(InvoiceValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/purchase-orders/{source}/{purchaseOrderNumber}/invoice-validations")
    public InvoiceValidationResponse validate(
            @PathVariable ClientSource source,
            @PathVariable String purchaseOrderNumber,
            @Valid @RequestBody InvoiceValidationRequest request
    ) {
        return validationService.validate(source, purchaseOrderNumber, request);
    }

    @GetMapping("/invoice-validations/{validationId}")
    public InvoiceValidationResponse findById(@PathVariable UUID validationId) {
        return validationService.findById(validationId);
    }
}

