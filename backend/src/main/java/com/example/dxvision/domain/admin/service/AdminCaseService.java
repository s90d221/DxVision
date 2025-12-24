package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.AdminCaseDiagnosisDto;
import com.example.dxvision.domain.admin.dto.AdminCaseFindingDto;
import com.example.dxvision.domain.admin.dto.AdminCaseRequest;
import com.example.dxvision.domain.admin.dto.AdminCaseResponse;
import com.example.dxvision.domain.admin.dto.DiagnosisWeightRequest;
import com.example.dxvision.domain.admin.dto.LesionDataRequest;
import com.example.dxvision.domain.casefile.CaseDiagnosis;
import com.example.dxvision.domain.casefile.CaseFinding;
import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.casefile.LesionShapeType;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCaseService {
    private final ImageCaseRepository imageCaseRepository;
    private final FindingRepository findingRepository;
    private final DiagnosisRepository diagnosisRepository;

    public AdminCaseService(
            ImageCaseRepository imageCaseRepository,
            FindingRepository findingRepository,
            DiagnosisRepository diagnosisRepository
    ) {
        this.imageCaseRepository = imageCaseRepository;
        this.findingRepository = findingRepository;
        this.diagnosisRepository = diagnosisRepository;
    }

    @Transactional
    public AdminCaseResponse createCase(AdminCaseRequest request) {
        validateLesionData(request.lesionShapeType(), request.lesionData());
        String lesionDataJson = buildLesionDataJson(request.lesionShapeType(), request.lesionData());

        ImageCase imageCase = new ImageCase(
                request.title(),
                request.description(),
                request.modality(),
                request.species(),
                request.imageUrl(),
                request.lesionShapeType(),
                lesionDataJson
        );

        applyFindingConfig(imageCase, request.findingOptionIds(), request.requiredFindingIds());
        applyDiagnosisConfig(imageCase, request.diagnosisOptionWeights());

        ImageCase saved = imageCaseRepository.save(imageCase);
        return toResponse(saved);
    }

    @Transactional
    public AdminCaseResponse updateCase(Long caseId, AdminCaseRequest request) {
        ImageCase imageCase = imageCaseRepository.findWithOptionsById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        validateLesionData(request.lesionShapeType(), request.lesionData());
        String lesionDataJson = buildLesionDataJson(request.lesionShapeType(), request.lesionData());

        boolean metadataChanged = !Objects.equals(imageCase.getTitle(), request.title())
                || !Objects.equals(imageCase.getDescription(), request.description())
                || imageCase.getModality() != request.modality()
                || imageCase.getSpecies() != request.species()
                || !Objects.equals(imageCase.getImageUrl(), request.imageUrl());

        Set<Long> previousRequired = imageCase.getFindings().stream()
                .filter(CaseFinding::isRequiredFinding)
                .map(cf -> cf.getFinding().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        Map<Long, Double> previousWeights = imageCase.getDiagnoses().stream()
                .collect(HashMap::new, (map, cd) -> map.put(cd.getDiagnosis().getId(), cd.getWeight()), HashMap::putAll);

        boolean lesionChanged = imageCase.getLesionShapeType() != request.lesionShapeType()
                || !Objects.equals(imageCase.getLesionDataJson(), lesionDataJson);

        Set<Long> optionIds = toOrderedSet(request.findingOptionIds());
        Set<Long> requiredIds = toOrderedSet(request.requiredFindingIds());
        Map<Long, Double> newWeights = toWeightMap(request.diagnosisOptionWeights());

        imageCase.updateMetadata(
                request.title(),
                request.description(),
                request.modality(),
                request.species(),
                request.imageUrl(),
                request.lesionShapeType(),
                lesionDataJson
        );

        imageCase.getFindings().clear();
        imageCase.getDiagnoses().clear();

        applyFindingConfig(imageCase, optionIds.stream().toList(), requiredIds.stream().toList());
        applyDiagnosisConfig(imageCase, request.diagnosisOptionWeights());

        boolean correctnessChanged = lesionChanged
                || !previousRequired.equals(requiredIds)
                || !previousWeights.equals(newWeights);

        if (metadataChanged || correctnessChanged) {
            imageCase.incrementVersion();
        }

        return toResponse(imageCase);
    }

    @Transactional(readOnly = true)
    public List<AdminCaseResponse> listCases() {
        return imageCaseRepository.findAllWithOptions().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCaseResponse getCase(Long id) {
        ImageCase imageCase = imageCaseRepository.findWithOptionsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        return toResponse(imageCase);
    }

    @Transactional
    public void deleteCase(Long id) {
        if (!imageCaseRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found");
        }
        imageCaseRepository.deleteById(id);
    }

    private void validateLesionData(LesionShapeType shapeType, LesionDataRequest lesionData) {
        if (shapeType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesion shape type is required");
        }
        if (lesionData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesion data is required");
        }
        if (shapeType != LesionShapeType.CIRCLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CIRCLE lesion type is supported");
        }
        if (lesionData.cx() < 0 || lesionData.cx() > 1 || lesionData.cy() < 0 || lesionData.cy() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesion coordinates must be between 0 and 1");
        }
        if (lesionData.r() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Radius must be greater than 0");
        }
    }

    private String buildLesionDataJson(LesionShapeType shapeType, LesionDataRequest lesionData) {
        return """
                {"type":"%s","cx":%s,"cy":%s,"r":%s}
                """.formatted(shapeType.name(), lesionData.cx(), lesionData.cy(), lesionData.r()).trim();
    }

    private void applyFindingConfig(ImageCase imageCase, List<Long> findingOptionIds, List<Long> requiredFindingIds) {
        Set<Long> optionIds = toOrderedSet(findingOptionIds);
        Set<Long> requiredIds = toOrderedSet(requiredFindingIds);

        if (!optionIds.containsAll(requiredIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requiredFindingIds must be subset of findingOptionIds");
        }

        if (optionIds.isEmpty()) {
            return;
        }

        List<Finding> findings = findingRepository.findAllById(optionIds);
        if (findings.size() != optionIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more findings not found");
        }

        for (Finding finding : findings) {
            boolean required = requiredIds.contains(finding.getId());
            imageCase.getFindings().add(new CaseFinding(imageCase, finding, required));
        }
    }

    private void applyDiagnosisConfig(ImageCase imageCase, List<DiagnosisWeightRequest> diagnosisWeights) {
        Map<Long, Double> weightMap = toWeightMap(diagnosisWeights);
        if (weightMap.isEmpty()) {
            return;
        }

        List<Diagnosis> diagnoses = diagnosisRepository.findAllById(weightMap.keySet());
        if (diagnoses.size() != weightMap.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more diagnoses not found");
        }

        for (Diagnosis diagnosis : diagnoses) {
            double weight = weightMap.get(diagnosis.getId());
            imageCase.getDiagnoses().add(new CaseDiagnosis(imageCase, diagnosis, weight));
        }
    }

    private Map<Long, Double> toWeightMap(List<DiagnosisWeightRequest> diagnosisWeights) {
        if (diagnosisWeights == null) {
            return Collections.emptyMap();
        }
        Map<Long, Double> weightMap = new HashMap<>();
        for (DiagnosisWeightRequest req : diagnosisWeights) {
            if (req.weight() == null || req.weight() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnosis weight must be greater than 0");
            }
            weightMap.put(req.diagnosisId(), req.weight());
        }
        return weightMap;
    }

    private Set<Long> toOrderedSet(List<Long> ids) {
        if (ids == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(ids);
    }

    private AdminCaseResponse toResponse(ImageCase imageCase) {
        List<AdminCaseFindingDto> findingDtos = imageCase.getFindings().stream()
                .map(cf -> new AdminCaseFindingDto(
                        cf.getFinding().getId(),
                        cf.getFinding().getLabel(),
                        cf.isRequiredFinding()))
                .toList();

        List<AdminCaseDiagnosisDto> diagnosisDtos = imageCase.getDiagnoses().stream()
                .map(cd -> new AdminCaseDiagnosisDto(
                        cd.getDiagnosis().getId(),
                        cd.getDiagnosis().getName(),
                        cd.getWeight()))
                .toList();

        return new AdminCaseResponse(
                imageCase.getId(),
                imageCase.getVersion(),
                imageCase.getTitle(),
                imageCase.getDescription(),
                imageCase.getModality(),
                imageCase.getSpecies(),
                imageCase.getImageUrl(),
                imageCase.getLesionShapeType(),
                imageCase.getLesionDataJson(),
                findingDtos,
                diagnosisDtos
        );
    }
}
