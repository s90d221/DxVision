package com.example.dxvision.domain.admin.dto;

import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import java.util.List;

public record AdminCaseUpsertRequest(
        String title,
        String description,
        Modality modality,
        Species species,
        String imageUrl,
        LesionDataDto lesionData,
        List<AdminFindingSelection> findings,
        List<AdminDiagnosisWeight> diagnoses,
        String expertFindingExplanation,
        String expertDiagnosisExplanation,
        String expertLocationExplanation
) {
}
