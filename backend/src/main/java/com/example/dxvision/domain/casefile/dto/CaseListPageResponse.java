package com.example.dxvision.domain.casefile.dto;

import java.util.List;

public record CaseListPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
