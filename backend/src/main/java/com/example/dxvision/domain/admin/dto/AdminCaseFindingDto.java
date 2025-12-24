package com.example.dxvision.domain.admin.dto;

public record AdminCaseFindingDto(
        Long findingId,
        String label,
        boolean required
) {
}
