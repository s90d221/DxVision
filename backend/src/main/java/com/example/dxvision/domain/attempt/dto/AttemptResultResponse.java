package com.example.dxvision.domain.attempt.dto;

import com.example.dxvision.domain.attempt.LocationGrade;
import java.util.List;

public record AttemptResultResponse(
        Long attemptId,
        Long caseId,
        Long caseVersion,
        double findingsScore,
        double locationScore,
        double diagnosisScore,
        double finalScore,
        String explanation,
        LocationGrade locationGrade,
        List<String> correctFindings,
        List<String> correctDiagnoses
) {
}
