package com.netflixclone.util;

import com.netflixclone.dto.response.PageResponse;
import org.springframework.data.domain.Page;

import java.util.Objects;

public final class PaginationUtils {

    private PaginationUtils() {
        // Utility class; do not create instances.
    }

    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        Objects.requireNonNull(page, "page must not be null");

        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }
}