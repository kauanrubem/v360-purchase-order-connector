package br.com.v360.purchaseorder.infrastructure.web;

import br.com.v360.purchaseorder.application.InvoiceValidationReportService;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.infrastructure.web.dto.InvoiceValidationReportResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/reports/invoice-validations")
public class InvoiceValidationReportController {

    private final InvoiceValidationReportService reportService;

    public InvoiceValidationReportController(InvoiceValidationReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public InvoiceValidationReportResponse report(
            @RequestParam(required = false) ClientSource source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return reportService.generate(source, from, to);
    }
}

