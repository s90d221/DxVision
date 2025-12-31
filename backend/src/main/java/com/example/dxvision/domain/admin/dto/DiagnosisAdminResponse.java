package com.example.dxvision.domain.admin.dto;

import java.util.List;

public record DiagnosisAdminResponse(
        Long id,
        String name,
        String description,
        List<Long> folderIds
) {
}
