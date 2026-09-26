package br.com.v360.purchaseorder.application;

import br.com.v360.purchaseorder.domain.exception.InvalidRequestException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.DivergenceType;
import br.com.v360.purchaseorder.domain.model.ValidationStatus;
import br.com.v360.purchaseorder.domain.repository.DivergenceCountProjection;
import br.com.v360.purchaseorder.domain.repository.InvoiceValidationRepository;
import br.com.v360.purchaseorder.domain.repository.InvoiceValidationReportEntryProjection;
import br.com.v360.purchaseorder.infrastructure.web.dto.InvoiceValidationReportResponse;
import br.com.v360.purchaseorder.infrastructure.web.dto.InvoiceValidationReportEntryResponse;
import br.com.v360.purchaseorder.infrastructure.web.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InvoiceValidationReportService {

    private static final Instant EARLIEST_SUPPORTED_INSTANT = Instant.parse("0001-01-01T00:00:00Z");
    private static final Instant LATEST_SUPPORTED_INSTANT = Instant.parse("9999-12-31T23:59:59.999999Z");

    private final InvoiceValidationRepository repository;

    public InvoiceValidationReportService(InvoiceValidationRepository repository) {
        this.repository = repository;
    }

    public InvoiceValidationReportResponse generate(
            ClientSource source,
            ValidationStatus status,
            Instant from,
            Instant to,
            Instant snapshotAt,
            Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("O início do período não pode ser posterior ao fim.");
        }

        Instant effectiveFrom = from == null ? EARLIEST_SUPPORTED_INSTANT : from;
        Instant requestedTo = to == null ? LATEST_SUPPORTED_INSTANT : to;
        Instant effectiveTo = requestedTo.isBefore(snapshotAt) ? requestedTo : snapshotAt;

        long total = repository.countForReport(source, status, effectiveFrom, effectiveTo);
        long approved = status == ValidationStatus.REJECTED
                ? 0
                : repository.countForReportByStatus(ValidationStatus.APPROVED, source, effectiveFrom, effectiveTo);
        long rejected = status == ValidationStatus.APPROVED
                ? 0
                : repository.countForReportByStatus(ValidationStatus.REJECTED, source, effectiveFrom, effectiveTo);

        Map<DivergenceType, Long> divergencesByType = new LinkedHashMap<>();
        for (DivergenceCountProjection count : repository.countDivergencesForReport(
                source,
                status,
                effectiveFrom,
                effectiveTo
        )) {
            divergencesByType.put(count.getType(), count.getTotal());
        }

        Page<InvoiceValidationReportEntryProjection> entries = repository.findReportEntries(
                source,
                status,
                effectiveFrom,
                effectiveTo,
                pageable
        );
        PageResponse<InvoiceValidationReportEntryResponse> page = PageResponse.from(
                entries.map(InvoiceValidationReportEntryResponse::from),
                snapshotAt
        );
        return new InvoiceValidationReportResponse(
                total,
                approved,
                rejected,
                divergencesByType,
                page.content(),
                page.page()
        );
    }
}
