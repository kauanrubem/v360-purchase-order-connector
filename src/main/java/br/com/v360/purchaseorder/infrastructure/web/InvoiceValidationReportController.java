package br.com.v360.purchaseorder.infrastructure.web;

import br.com.v360.purchaseorder.application.InvoiceValidationReportService;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.ValidationStatus;
import br.com.v360.purchaseorder.infrastructure.web.dto.InvoiceValidationReportResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.Clock;

@Validated
@RestController
@RequestMapping("/api/v1/reports/invoice-validations")
public class InvoiceValidationReportController {

    private final InvoiceValidationReportService reportService;
    private final Clock clock;

    public InvoiceValidationReportController(InvoiceValidationReportService reportService, Clock clock) {
        this.reportService = reportService;
        this.clock = clock;
    }

    @GetMapping
    public InvoiceValidationReportResponse report(
            @RequestParam(required = false) ClientSource source,
            @RequestParam(required = false) ValidationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant snapshotAt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Instant effectiveSnapshotAt = snapshotAt == null ? clock.instant() : snapshotAt;
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("validatedAt"), Sort.Order.desc("id"))
        );
        return reportService.generate(source, status, from, to, effectiveSnapshotAt, pageable);
    }
}
