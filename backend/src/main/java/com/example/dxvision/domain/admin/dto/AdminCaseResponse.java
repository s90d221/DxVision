package com.example.dxvision.domain.admin.dto;

import com.example.dxvision.domain.casefile.LesionShapeType;
import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import java.time.Instant;
import java.util.List;

public record AdminCaseResponse(
        Long id,
        Long version,
        String title,
        String description,
        Modality modality,
        Species species,
        String imageUrl,
        LesionShapeType lesionShapeType,
        LesionDataDto lesionData,
        String lesionDataJson,
        List<AdminCaseFindingDto> findings,
        List<AdminCaseDiagnosisDto> diagnoses,
        Instant deletedAt,
        Instant updatedAt
) {
}
