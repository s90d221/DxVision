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
import org.springframework.security.core.Authentication;
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

        Set<Long> normalizedFindingIds = new HashSet<>(request.findingIds());
        Set<Long> normalizedDiagnosisIds = new HashSet<>(request.diagnosisIds());

        LocationEvaluation locationEvaluation = evaluateLocation(imageCase, request.clickX(), request.clickY());
        ScoredFindings findingsScore = evaluateFindingsPercent(imageCase, normalizedFindingIds);
        ScoredDiagnoses diagnosisScore = evaluateDiagnosesPercent(imageCase, normalizedDiagnosisIds);
        double finalScore = 0.3 * findingsScore.score + 0.3 * locationEvaluation.score + 0.4 * diagnosisScore.score;

        Attempt attempt = new Attempt(user, imageCase, Instant.now());
        attempt.recordScores(findingsScore.score, locationEvaluation.score, diagnosisScore.score, finalScore, locationEvaluation.explanation);

        AttemptLocationAnswer locationAnswer = new AttemptLocationAnswer(
                attempt,
                request.clickX(),
                request.clickY(),
                locationEvaluation.grade
        );
        attempt.attachLocationAnswer(locationAnswer);

        for (Long findingId : normalizedFindingIds) {
            imageCase.getFindings().stream()
                    .filter(cf -> cf.getFinding().getId().equals(findingId))
                    .findFirst()
                    .ifPresent(cf -> attempt.addFindingAnswer(new AttemptFindingAnswer(attempt, cf.getFinding())));
        }

        for (Long diagnosisId : normalizedDiagnosisIds) {
            imageCase.getDiagnoses().stream()
                    .filter(cd -> cd.getDiagnosis().getId().equals(diagnosisId))
                    .findFirst()
                    .ifPresent(cd -> attempt.addDiagnosisAnswer(new AttemptDiagnosisAnswer(attempt, cd.getDiagnosis())));
        }

        String explanation = buildExplanation(
                imageCase,
                normalizedFindingIds,
                normalizedDiagnosisIds,
                findingsScore,
                diagnosisScore,
                locationEvaluation
        );

        Attempt saved = attemptRepository.save(attempt);

        return new AttemptResultResponse(
                saved.getId(),
                imageCase.getId(),
                imageCase.getVersion(),
                findingsScore.score,
                locationEvaluation.score,
                diagnosisScore.score,
                finalScore,
                explanation,
                locationEvaluation.grade,
                findingsScore.correctLabels,
                diagnosisScore.correctNames
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        Object principal = authentication.getPrincipal();
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
                score = 100.0;
            } else if (distance <= r * 1.5) {
                grade = LocationGrade.NEAR;
                score = 70.0;
            } else if (distance <= r * 2.5) {
                grade = LocationGrade.FAR;
                score = 30.0;
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

    private ScoredFindings evaluateFindingsPercent(ImageCase imageCase, Set<Long> selected) {
        Map<Long, String> labelById = new HashMap<>();
        Set<Long> required = new HashSet<>();
        Set<Long> caseFindingIds = new HashSet<>();

        for (CaseFinding cf : imageCase.getFindings()) {
            Long id = cf.getFinding().getId();
            labelById.put(id, cf.getFinding().getLabel());
            caseFindingIds.add(id);
            if (cf.isRequiredFinding()) {
                required.add(id);
            }
        }

        long correctCount = selected.stream().filter(required::contains).count();
        long wrongCount = selected.stream().filter(id -> !required.contains(id)).count();
        long invalidCount = selected.stream().filter(id -> !caseFindingIds.contains(id)).count();
        double correctRate = required.isEmpty() ? 0.0 : (double) correctCount / required.size();
        double wrongRate = selected.isEmpty() ? 0.0 : (double) wrongCount / selected.size();
        double score = Math.max(0, correctRate - 0.5 * wrongRate) * 100.0;

        List<String> correctLabels = required.stream().map(labelById::get).toList();
        return new ScoredFindings(score, correctLabels, invalidCount);
    }

    private ScoredDiagnoses evaluateDiagnosesPercent(ImageCase imageCase, Set<Long> selectedIds) {
        Map<Long, Double> weightByDiagnosis = new HashMap<>();
        Map<Long, String> nameById = new HashMap<>();
        for (CaseDiagnosis cd : imageCase.getDiagnoses()) {
            weightByDiagnosis.put(cd.getDiagnosis().getId(), cd.getWeight());
            nameById.put(cd.getDiagnosis().getId(), cd.getDiagnosis().getName());
        }

        double totalWeight = weightByDiagnosis.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight == 0) {
            return new ScoredDiagnoses(0.0, List.of(), 0);
        }

        double selectedWeight = selectedIds.stream()
                .filter(weightByDiagnosis::containsKey)
                .mapToDouble(weightByDiagnosis::get)
                .sum();

        long invalidCount = selectedIds.stream().filter(id -> !weightByDiagnosis.containsKey(id)).count();

        double ratio = selectedWeight / totalWeight;
        double score = Math.min(100.0, Math.max(0.0, ratio * 100.0));
        List<String> correctNames = weightByDiagnosis.keySet().stream().map(nameById::get).toList();
        return new ScoredDiagnoses(score, correctNames, invalidCount);
    }

    private String buildExplanation(
            ImageCase imageCase,
            Set<Long> selectedFindingIds,
            Set<Long> selectedDiagnosisIds,
            ScoredFindings findings,
            ScoredDiagnoses diagnoses,
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
        int extraFindings = (int) selectedFindingSet.stream().filter(id -> !requiredIds.contains(id)).count();

        Set<Long> correctDiagIds = imageCase.getDiagnoses().stream()
                .map(cd -> cd.getDiagnosis().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        int matchedDiag = (int) selectedDiagnosisSet.stream().filter(correctDiagIds::contains).count();
        long invalidDiag = diagnoses.invalidCount;

        return """
                Findings: matched %d, missing %d, extra %d, invalid %d.
                Diagnoses: matched %d, invalid %d.
                %s
                """.formatted(
                matchedFindings,
                missingFindings,
                extraFindings,
                findings.invalidCount,
                matchedDiag,
                invalidDiag,
                locationEvaluation.explanation
        );
    }

    private record LocationEvaluation(LocationGrade grade, double score, String explanation) {
    }

    private record ScoredFindings(double score, List<String> correctLabels, long invalidCount) {
    }

    private record ScoredDiagnoses(double score, List<String> correctNames, long invalidCount) {
    }
}
