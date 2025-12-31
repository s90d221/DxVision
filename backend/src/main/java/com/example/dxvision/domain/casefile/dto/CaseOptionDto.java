package com.example.dxvision.domain.casefile.dto;

import com.example.dxvision.domain.casefile.LesionShapeType;
import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import java.util.List;

public record CaseOptionDto(
        Long id,
        Long version,
        String title,
        String description,
        Modality modality,
        Species species,
        String imageUrl,
        LesionShapeType lesionShapeType,
        List<FindingOptionDto> findings,
        List<DiagnosisOptionDto> diagnoses,
        List<OptionFolderResponse> findingFolders,
        List<OptionFolderResponse> diagnosisFolders
) {
}
