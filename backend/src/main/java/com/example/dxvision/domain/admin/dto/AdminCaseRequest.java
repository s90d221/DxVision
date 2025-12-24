package com.example.dxvision.domain.admin.dto;

import com.example.dxvision.domain.casefile.LesionShapeType;
import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AdminCaseRequest(
        @NotBlank(message = "Title is required")
        String title,
        String description,
        @NotNull(message = "Modality is required")
        Modality modality,
        @NotNull(message = "Species is required")
        Species species,
        @NotBlank(message = "Image URL is required")
        String imageUrl,
        @NotNull(message = "Lesion shape type is required")
        LesionShapeType lesionShapeType,
        @Valid
        LesionDataRequest lesionData,
        List<Long> findingOptionIds,
        List<Long> requiredFindingIds,
        List<DiagnosisWeightRequest> diagnosisOptionWeights
) {
}
