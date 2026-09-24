package br.com.v360.purchaseorder.application;

import br.com.v360.purchaseorder.domain.exception.InvalidRequestException;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.DivergenceType;
import br.com.v360.purchaseorder.domain.model.ValidationStatus;
import br.com.v360.purchaseorder.domain.repository.DivergenceCountProjection;
import br.com.v360.purchaseorder.domain.repository.InvoiceValidationRepository;
import br.com.v360.purchaseorder.infrastructure.web.dto.InvoiceValidationReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InvoiceValidationReportService {

    private final InvoiceValidationRepository repository;

    public InvoiceValidationReportService(InvoiceValidationRepository repository) {
        this.repository = repository;
    }

    public InvoiceValidationReportResponse generate(ClientSource source, Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("O início do período não pode ser posterior ao fim.");
        }

        long total = repository.countForReport(source, from, to);
        long approved = repository.countForReportByStatus(ValidationStatus.APPROVED, source, from, to);
        long rejected = repository.countForReportByStatus(ValidationStatus.REJECTED, source, from, to);

        Map<DivergenceType, Long> divergencesByType = new LinkedHashMap<>();
        for (DivergenceCountProjection count : repository.countDivergencesForReport(source, from, to)) {
            divergencesByType.put(count.getType(), count.getTotal());
        }

        return new InvoiceValidationReportResponse(total, approved, rejected, divergencesByType);
    }
}

