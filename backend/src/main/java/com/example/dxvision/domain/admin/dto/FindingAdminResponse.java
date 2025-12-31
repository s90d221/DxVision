package com.example.dxvision.domain.admin.dto;

import java.util.List;

public record FindingAdminResponse(
        Long id,
        String label,
        String description,
        List<Long> folderIds
) {
}
