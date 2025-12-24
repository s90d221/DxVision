package com.example.dxvision.domain.admin.dto;

import java.util.List;

public record LookupResponse(
        List<LookupFindingDto> findings,
        List<LookupDiagnosisDto> diagnoses
) {
}
