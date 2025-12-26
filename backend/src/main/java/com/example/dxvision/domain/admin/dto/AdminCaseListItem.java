package com.example.dxvision.domain.admin.dto;

import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import java.time.Instant;

public record AdminCaseListItem(
        Long id,
        Long version,
        String title,
        Modality modality,
        Species species,
        Instant deletedAt,
        Instant updatedAt
) {
}
