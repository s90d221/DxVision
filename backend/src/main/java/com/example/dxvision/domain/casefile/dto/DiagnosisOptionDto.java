package com.example.dxvision.domain.casefile.dto;

public record DiagnosisOptionDto(
        Long id,
        String name,
        Long folderId,
        String folderName,
        Integer orderIndex,
        Integer folderOrderIndex
) {
}
