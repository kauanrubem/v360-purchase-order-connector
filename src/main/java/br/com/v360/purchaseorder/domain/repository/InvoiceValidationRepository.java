package br.com.v360.purchaseorder.domain.repository;

import br.com.v360.purchaseorder.domain.model.InvoiceValidation;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.ValidationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceValidationRepository extends JpaRepository<InvoiceValidation, UUID> {

    @Override
    @EntityGraph(attributePaths = {"invoiceItems", "divergences"})
    Optional<InvoiceValidation> findById(UUID id);

    @Query("""
            select count(validation)
            from InvoiceValidation validation
            where (:source is null or validation.source = :source)
              and (:from is null or validation.validatedAt >= :from)
              and (:to is null or validation.validatedAt <= :to)
            """)
    long countForReport(
            @Param("source") ClientSource source,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            select count(validation)
            from InvoiceValidation validation
            where validation.status = :status
              and (:source is null or validation.source = :source)
              and (:from is null or validation.validatedAt >= :from)
              and (:to is null or validation.validatedAt <= :to)
            """)
    long countForReportByStatus(
            @Param("status") ValidationStatus status,
            @Param("source") ClientSource source,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            select divergence.type as type, count(divergence) as total
            from Divergence divergence
            join divergence.validation validation
            where (:source is null or validation.source = :source)
              and (:from is null or validation.validatedAt >= :from)
              and (:to is null or validation.validatedAt <= :to)
            group by divergence.type
            order by divergence.type
            """)
    List<DivergenceCountProjection> countDivergencesForReport(
            @Param("source") ClientSource source,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
