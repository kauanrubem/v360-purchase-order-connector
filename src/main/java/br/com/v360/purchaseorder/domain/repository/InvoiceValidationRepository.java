package br.com.v360.purchaseorder.domain.repository;

import br.com.v360.purchaseorder.domain.model.InvoiceValidation;
import br.com.v360.purchaseorder.domain.model.ClientSource;
import br.com.v360.purchaseorder.domain.model.ValidationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
              and (:status is null or validation.status = :status)
              and validation.validatedAt >= :from
              and validation.validatedAt <= :to
            """)
    long countForReport(
            @Param("source") ClientSource source,
            @Param("status") ValidationStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            select count(validation)
            from InvoiceValidation validation
            where validation.status = :status
              and (:source is null or validation.source = :source)
              and validation.validatedAt >= :from
              and validation.validatedAt <= :to
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
              and (:status is null or validation.status = :status)
              and validation.validatedAt >= :from
              and validation.validatedAt <= :to
            group by divergence.type
            order by divergence.type
            """)
    List<DivergenceCountProjection> countDivergencesForReport(
            @Param("source") ClientSource source,
            @Param("status") ValidationStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(
            value = """
                    select validation.id as id,
                           validation.source as source,
                           validation.purchaseOrderNumber as purchaseOrderNumber,
                           validation.vendorTaxId as vendorTaxId,
                           validation.status as status,
                           validation.validatedAt as validatedAt,
                           size(validation.divergences) as divergenceCount
                    from InvoiceValidation validation
                    where (:source is null or validation.source = :source)
                      and (:status is null or validation.status = :status)
                      and validation.validatedAt >= :from
                      and validation.validatedAt <= :to
                    """,
            countQuery = """
                    select count(validation)
                    from InvoiceValidation validation
                    where (:source is null or validation.source = :source)
                      and (:status is null or validation.status = :status)
                      and validation.validatedAt >= :from
                      and validation.validatedAt <= :to
                    """
    )
    Page<InvoiceValidationReportEntryProjection> findReportEntries(
            @Param("source") ClientSource source,
            @Param("status") ValidationStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
