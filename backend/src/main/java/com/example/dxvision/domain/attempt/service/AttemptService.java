package com.example.dxvision.domain.attempt.service;

import com.example.dxvision.domain.attempt.Attempt;
import com.example.dxvision.domain.attempt.AttemptDiagnosisAnswer;
import com.example.dxvision.domain.attempt.AttemptFindingAnswer;
import com.example.dxvision.domain.attempt.AttemptLocationAnswer;
import com.example.dxvision.domain.attempt.LocationGrade;
import com.example.dxvision.domain.attempt.dto.AttemptResultResponse;
import com.example.dxvision.domain.attempt.dto.AttemptSubmitRequest;
import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.auth.security.CustomUserDetails;
import com.example.dxvision.domain.casefile.CaseDiagnosis;
import com.example.dxvision.domain.casefile.CaseFinding;
import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.repository.AttemptRepository;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import com.example.dxvision.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttemptService {
    private final UserRepository userRepository;
    private final ImageCaseRepository imageCaseRepository;
    private final AttemptRepository attemptRepository;
    private final ObjectMapper objectMapper;

    public AttemptService(
            UserRepository userRepository,
            ImageCaseRepository imageCaseRepository,
            AttemptRepository attemptRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.imageCaseRepository = imageCaseRepository;
        this.attemptRepository = attemptRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AttemptResultResponse submitAttempt(AttemptSubmitRequest request) {
        User user = getCurrentUser();
        ImageCase imageCase = imageCaseRepository.findById(request.caseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        if (!imageCase.getVersion().equals(request.caseVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Case version mismatch");
        }

        LocationEvaluation locationEvaluation = evaluateLocation(imageCase, request.clickX(), request.clickY());
        double findingsScore = evaluateFindings(imageCase, request.findingIds());
        double diagnosisScore = evaluateDiagnoses(imageCase, request.diagnosisIds());
        double finalScore = 0.4 * findingsScore + 0.3 * locationEvaluation.score + 0.3 * diagnosisScore;

        Attempt attempt = new Attempt(user, imageCase, Instant.now());
        attempt.recordScores(findingsScore, locationEvaluation.score, diagnosisScore, finalScore, locationEvaluation.explanation);

        AttemptLocationAnswer locationAnswer = new AttemptLocationAnswer(
                attempt,
                request.clickX(),
                request.clickY(),
                locationEvaluation.grade
        );
        attempt.attachLocationAnswer(locationAnswer);

        for (Long findingId : request.findingIds()) {
            imageCase.getFindings().stream()
                    .filter(cf -> cf.getFinding().getId().equals(findingId))
                    .findFirst()
                    .ifPresent(cf -> attempt.addFindingAnswer(new AttemptFindingAnswer(attempt, cf.getFinding())));
        }

        for (Long diagnosisId : request.diagnosisIds()) {
            imageCase.getDiagnoses().stream()
                    .filter(cd -> cd.getDiagnosis().getId().equals(diagnosisId))
                    .findFirst()
                    .ifPresent(cd -> attempt.addDiagnosisAnswer(new AttemptDiagnosisAnswer(attempt, cd.getDiagnosis())));
        }

        Attempt saved = attemptRepository.save(attempt);

        String explanation = buildExplanation(
                imageCase,
                request.findingIds(),
                request.diagnosisIds(),
                locationEvaluation
        );

        return new AttemptResultResponse(
                saved.getId(),
                imageCase.getId(),
                imageCase.getVersion(),
                findingsScore,
                locationEvaluation.score,
                diagnosisScore,
                finalScore,
                explanation,
                locationEvaluation.grade
        );
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return userRepository.findById(customUserDetails.getUser().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    private LocationEvaluation evaluateLocation(ImageCase imageCase, double clickX, double clickY) {
        try {
            JsonNode node = objectMapper.readTree(imageCase.getLesionDataJson());
            double cx = node.get("cx").asDouble();
            double cy = node.get("cy").asDouble();
            double r = node.get("r").asDouble();
            double dx = clickX - cx;
            double dy = clickY - cy;
            double distance = Math.sqrt(dx * dx + dy * dy);

            LocationGrade grade;
            double score;
            if (distance <= r) {
                grade = LocationGrade.INSIDE;
                score = 1.0;
            } else if (distance <= r * 1.5) {
                grade = LocationGrade.NEAR;
                score = 0.7;
            } else if (distance <= r * 2.5) {
                grade = LocationGrade.FAR;
                score = 0.3;
            } else {
                grade = LocationGrade.WRONG;
                score = 0.0;
            }
            String explanation = "Location grade: %s (distance=%.3f, radius=%.3f)".formatted(grade, distance, r);
            return new LocationEvaluation(grade, score, explanation);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lesion data");
        }
    }

    private double evaluateFindings(ImageCase imageCase, List<Long> selectedIds) {
        Set<Long> required = imageCase.getFindings().stream()
                .filter(CaseFinding::isRequiredFinding)
                .map(cf -> cf.getFinding().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Long> selected = new HashSet<>(selectedIds);

        long tp = selected.stream().filter(required::contains).count();
        long fp = selected.stream().filter(id -> !required.contains(id)).count();
        long fn = required.stream().filter(id -> !selected.contains(id)).count();

        if (tp == 0) {
            return 0.0;
        }
        double f1 = (2.0 * tp) / (2.0 * tp + fp + fn);
        return f1;
    }

    private double evaluateDiagnoses(ImageCase imageCase, List<Long> selectedIds) {
        Map<Long, Double> weightByDiagnosis = new HashMap<>();
        for (CaseDiagnosis cd : imageCase.getDiagnoses()) {
            weightByDiagnosis.put(cd.getDiagnosis().getId(), cd.getWeight());
        }

        double totalWeight = weightByDiagnosis.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight == 0) {
            return 0.0;
        }

        double selectedWeight = selectedIds.stream()
                .filter(weightByDiagnosis::containsKey)
                .mapToDouble(weightByDiagnosis::get)
                .sum();

        double ratio = selectedWeight / totalWeight;
        return Math.min(1.0, Math.max(0.0, ratio));
    }

    private String buildExplanation(
            ImageCase imageCase,
            List<Long> selectedFindingIds,
            List<Long> selectedDiagnosisIds,
            LocationEvaluation locationEvaluation
    ) {
        Set<Long> requiredIds = imageCase.getFindings().stream()
                .filter(CaseFinding::isRequiredFinding)
                .map(cf -> cf.getFinding().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        Set<Long> selectedFindingSet = new HashSet<>(selectedFindingIds);
        Set<Long> selectedDiagnosisSet = new HashSet<>(selectedDiagnosisIds);

        int matchedFindings = (int) selectedFindingSet.stream().filter(requiredIds::contains).count();
        int missingFindings = (int) requiredIds.stream().filter(id -> !selectedFindingSet.contains(id)).count();

        Set<Long> correctDiagIds = imageCase.getDiagnoses().stream()
                .map(cd -> cd.getDiagnosis().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        int matchedDiag = (int) selectedDiagnosisSet.stream().filter(correctDiagIds::contains).count();

        return """
                Findings: matched %d, missing %d.
                Diagnoses: matched %d.
                %s
                """.formatted(
                matchedFindings,
                missingFindings,
                matchedDiag,
                locationEvaluation.explanation
        );
    }

    private record LocationEvaluation(LocationGrade grade, double score, String explanation) {
    }
}
