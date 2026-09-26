package br.com.v360.purchaseorder.infrastructure.web.dto;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

public record PageResponse<T>(List<T> content, Metadata page) {

    public static <T> PageResponse<T> from(Page<T> source) {
        return from(source, null);
    }

    public static <T> PageResponse<T> from(Page<T> source, Instant snapshotAt) {
        return new PageResponse<>(
                source.getContent(),
                new Metadata(
                        source.getNumber(),
                        source.getSize(),
                        source.getTotalElements(),
                        source.getTotalPages(),
                        source.hasNext(),
                        source.hasPrevious(),
                        snapshotAt
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
            Instant snapshotAt
    ) {
    }
}
