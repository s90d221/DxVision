package com.example.dxvision.domain.admin.dto;

public record DiagnosisAdminResponse(
        Long id,
        String name,
        String description,
        Long folderId,
        String folderName,
        Integer orderIndex
) {
}
