package com.example.dxvision.domain.admin.dto;

public record AdminCaseDiagnosisDto(
        Long diagnosisId,
        String name,
        double weight
) {
}
