package com.example.dxvision.domain.casefile.dto;

public record OptionFolderItemDto(
        Long id,
        String label,
        String description,
        Integer sortOrder
) {
}
