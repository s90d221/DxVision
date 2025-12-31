package com.example.dxvision.domain.casefile.dto;

import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import com.example.dxvision.domain.progress.UserCaseStatus;
import java.time.Instant;

public record CaseListItemResponse(
        Long caseId,
        String title,
        Modality modality,
        Species species,
        Instant updatedAt,
        UserCaseStatus status,
        Instant lastAttemptAt,
        Double lastScore
) {
}
