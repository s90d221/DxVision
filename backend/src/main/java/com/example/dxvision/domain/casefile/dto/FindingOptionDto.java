package com.example.dxvision.domain.casefile.dto;

public record FindingOptionDto(
        Long id,
        String label,
        Long folderId,
        String folderName,
        Integer orderIndex,
        Integer folderOrderIndex
) {
}
