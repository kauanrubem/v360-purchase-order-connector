package br.com.v360.purchaseorder.infrastructure.web.dto;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PageResponse<T>(List<T> content, Metadata page) {

    public static <T> PageResponse<T> from(Page<T> source) {
        return from(source, null);
    }

    public static <T> PageResponse<T> from(Page<T> source, Instant snapshotAt) {
        return from(source, snapshotAt, null);
    }

    public static <T> PageResponse<T> from(Page<T> source, Instant snapshotAt, UUID snapshotId) {
        return new PageResponse<>(
                source.getContent(),
                new Metadata(
                        source.getNumber(),
                        source.getSize(),
                        source.getTotalElements(),
                        source.getTotalPages(),
                        source.hasNext(),
                        source.hasPrevious(),
                        snapshotAt,
                        snapshotId
                )
        );
    }

    public record Metadata(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious,
            Instant snapshotAt,
            UUID snapshotId
    ) {
    }
}
