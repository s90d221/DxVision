package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.AdminCaseDiagnosisDto;
import com.example.dxvision.domain.admin.dto.AdminCaseFindingDto;
import com.example.dxvision.domain.admin.dto.AdminCaseListItem;
import com.example.dxvision.domain.admin.dto.AdminCaseResponse;
import com.example.dxvision.domain.admin.dto.AdminCaseUpsertRequest;
import com.example.dxvision.domain.admin.dto.AdminDiagnosisWeight;
import com.example.dxvision.domain.admin.dto.AdminFindingSelection;
import com.example.dxvision.domain.admin.dto.LesionDataDto;
import com.example.dxvision.domain.admin.dto.PageResponse;
import com.example.dxvision.domain.casefile.CaseDiagnosis;
import com.example.dxvision.domain.casefile.CaseFinding;
import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.casefile.LesionShapeType;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import com.example.dxvision.global.storage.FileStorageService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCaseService {
    private final ImageCaseRepository imageCaseRepository;
    private final FindingRepository findingRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final FileStorageService fileStorageService;

    public AdminCaseService(
            ImageCaseRepository imageCaseRepository,
            FindingRepository findingRepository,
            DiagnosisRepository diagnosisRepository,
            FileStorageService fileStorageService
    ) {
        this.imageCaseRepository = imageCaseRepository;
        this.findingRepository = findingRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public AdminCaseResponse createCase(AdminCaseUpsertRequest request) {
        validateRequest(request, true);

        String lesionDataJson = buildLesionDataJson(request.lesionData());

        ImageCase imageCase = new ImageCase(
                request.title(),
                request.description(),
                request.modality(),
                request.species(),
                request.imageUrl(),
                LesionShapeType.CIRCLE,
                lesionDataJson
        );

        imageCase.replaceFindings(applyFindingConfig(request.findings()));
        imageCase.replaceDiagnoses(applyDiagnosisConfig(request.diagnoses()));

        ImageCase saved = imageCaseRepository.save(imageCase);
        return toResponse(saved);
    }

    @Transactional
    public AdminCaseResponse updateCase(Long caseId, AdminCaseUpsertRequest request) {
        ImageCase imageCase = imageCaseRepository.findWithOptionsById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        validateRequest(request, false);
        String lesionDataJson = buildLesionDataJson(request.lesionData());

        String previousImageUrl = imageCase.getImageUrl();
        String nextImageUrl = StringUtils.hasText(request.imageUrl()) ? request.imageUrl() : previousImageUrl;

        boolean metadataChanged = !Objects.equals(imageCase.getTitle(), request.title())
                || !Objects.equals(imageCase.getDescription(), request.description())
                || imageCase.getModality() != request.modality()
                || imageCase.getSpecies() != request.species()
                || !Objects.equals(previousImageUrl, nextImageUrl);

        Set<Long> previousRequired = imageCase.getFindings().stream()
                .filter(CaseFinding::isRequiredFinding)
                .map(cf -> cf.getFinding().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        Map<Long, Double> previousWeights = imageCase.getDiagnoses().stream()
                .collect(HashMap::new, (map, cd) -> map.put(cd.getDiagnosis().getId(), cd.getWeight()), HashMap::putAll);

        boolean lesionChanged = !Objects.equals(imageCase.getLesionDataJson(), lesionDataJson);

        imageCase.updateMetadata(
                request.title(),
                request.description(),
                request.modality(),
                request.species(),
                nextImageUrl,
                LesionShapeType.CIRCLE,
                lesionDataJson
        );

        imageCase.replaceFindings(applyFindingConfig(request.findings()));
        imageCase.replaceDiagnoses(applyDiagnosisConfig(request.diagnoses()));

        Set<Long> requiredIds = toOrderedSet(
                request.findings() == null
                        ? Collections.emptyList()
                        : request.findings().stream().filter(AdminFindingSelection::required).map(AdminFindingSelection::findingId).toList()
        );
        Map<Long, Double> newWeights = toWeightMap(request.diagnoses());

        boolean correctnessChanged = lesionChanged
                || !previousRequired.equals(requiredIds)
                || !previousWeights.equals(newWeights);

        if (metadataChanged || correctnessChanged) {
            imageCase.incrementVersion();
        }

        if (!Objects.equals(previousImageUrl, nextImageUrl)) {
            fileStorageService.deleteIfLocal(previousImageUrl);
        }

        return toResponse(imageCase);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCaseListItem> listCases(Pageable pageable, boolean includeDeleted) {
        Page<ImageCase> page = includeDeleted
                ? imageCaseRepository.findAllIncludingDeleted(pageable)
                : imageCaseRepository.findAll(pageable);
        Page<AdminCaseListItem> mapped = page.map(ic -> new AdminCaseListItem(
                ic.getId(),
                ic.getVersion(),
                ic.getTitle(),
                ic.getModality(),
                ic.getSpecies(),
                ic.getDeletedAt(),
                ic.getUpdatedAt()
        ));
        return PageResponse.of(mapped);
    }

    @Transactional(readOnly = true)
    public AdminCaseResponse getCase(Long id, boolean includeDeleted) {
        ImageCase imageCase = (includeDeleted
                ? imageCaseRepository.findByIdIncludingDeleted(id)
                : imageCaseRepository.findWithOptionsById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        return toResponse(imageCase);
    }

    @Transactional
    public void deleteCase(Long id) {
        ImageCase imageCase = imageCaseRepository.findWithOptionsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        imageCase.softDelete();
    }

    @Transactional
    public void restoreCase(Long id) {
        ImageCase imageCase = imageCaseRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        imageCase.restore();
    }

    private void validateRequest(AdminCaseUpsertRequest request, boolean imageRequired) {
        if (!StringUtils.hasText(request.title())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (request.modality() == null || request.species() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modality and species are required");
        }
        if (imageRequired && !StringUtils.hasText(request.imageUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is required");
        }
        validateLesionData(request.lesionData());
        validateFindings(request.findings());
        validateDiagnoses(request.diagnoses());
    }

    private void validateLesionData(LesionDataDto lesionData) {
        if (lesionData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesion data is required");
        }
        if (lesionData.cx() < 0 || lesionData.cx() > 1 || lesionData.cy() < 0 || lesionData.cy() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesion coordinates must be between 0 and 1");
        }
        if (lesionData.r() <= 0 || lesionData.r() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Radius must be between 0 and 1");
        }
    }

    private void validateFindings(List<AdminFindingSelection> selections) {
        if (selections == null) {
            return;
        }
        Set<Long> optionIds = toOrderedSet(selections.stream().map(AdminFindingSelection::findingId).toList());
        Set<Long> requiredIds = toOrderedSet(selections.stream()
                .filter(AdminFindingSelection::required)
                .map(AdminFindingSelection::findingId)
                .toList());
        if (!optionIds.containsAll(requiredIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required findings must be part of the selection");
        }
        if (!optionIds.isEmpty()) {
            List<Finding> findings = findingRepository.findAllById(optionIds);
            if (findings.size() != optionIds.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more findings not found");
            }
        }
    }

    private void validateDiagnoses(List<AdminDiagnosisWeight> diagnoses) {
        if (diagnoses == null) {
            return;
        }
        Map<Long, Double> weights = toWeightMap(diagnoses);
        if (!weights.isEmpty()) {
            List<Diagnosis> diagnosisList = diagnosisRepository.findAllById(weights.keySet());
            if (diagnosisList.size() != weights.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more diagnoses not found");
            }
        }
    }

    private String buildLesionDataJson(LesionDataDto lesionData) {
        return """
                {"type":"%s","cx":%s,"cy":%s,"r":%s}
                """.formatted(LesionShapeType.CIRCLE.name(), lesionData.cx(), lesionData.cy(), lesionData.r()).trim();
    }

    private List<CaseFinding> applyFindingConfig(List<AdminFindingSelection> selections) {
        if (selections == null || selections.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, AdminFindingSelection> selectionMap = new LinkedHashMap<>();
        for (AdminFindingSelection selection : selections) {
            if (selectionMap.put(selection.findingId(), selection) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate finding selection is not allowed");
            }
        }

        List<Finding> findings = findingRepository.findAllById(selectionMap.keySet());
        if (findings.size() != selectionMap.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more findings not found");
        }

        List<CaseFinding> newFindings = new ArrayList<>();
        for (Finding finding : findings) {
            AdminFindingSelection selection = selectionMap.get(finding.getId());
            newFindings.add(new CaseFinding(finding, selection.required()));
        }

        return newFindings;
    }

    private List<CaseDiagnosis> applyDiagnosisConfig(List<AdminDiagnosisWeight> diagnosisWeights) {
        Map<Long, Double> weightMap = toWeightMap(diagnosisWeights);
        if (weightMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Diagnosis> diagnoses = diagnosisRepository.findAllById(weightMap.keySet());
        if (diagnoses.size() != weightMap.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more diagnoses not found");
        }

        List<CaseDiagnosis> newDiagnoses = new ArrayList<>();
        for (Diagnosis diagnosis : diagnoses) {
            double weight = weightMap.get(diagnosis.getId());
            newDiagnoses.add(new CaseDiagnosis(diagnosis, weight));
        }

        return newDiagnoses;
    }

    private Map<Long, Double> toWeightMap(List<AdminDiagnosisWeight> diagnosisWeights) {
        if (diagnosisWeights == null) {
            return Collections.emptyMap();
        }
        Map<Long, Double> weightMap = new LinkedHashMap<>();
        double total = 0.0;
        for (AdminDiagnosisWeight req : diagnosisWeights) {
            if (req.weight() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnosis weight must be >= 0");
            }
            if (weightMap.containsKey(req.diagnosisId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate diagnosis weight is not allowed");
            }
            weightMap.put(req.diagnosisId(), req.weight());
            total += req.weight();
        }
        if (!weightMap.isEmpty() && total <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total diagnosis weight must be greater than 0");
        }
        if (total > 0) {
            // Normalize weights to keep scoring stable
            for (Map.Entry<Long, Double> entry : weightMap.entrySet()) {
                entry.setValue(entry.getValue() / total);
            }
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

        LesionDataDto lesionData = parseLesionData(imageCase.getLesionDataJson());

        return new AdminCaseResponse(
                imageCase.getId(),
                imageCase.getVersion(),
                imageCase.getTitle(),
                imageCase.getDescription(),
                imageCase.getModality(),
                imageCase.getSpecies(),
                imageCase.getImageUrl(),
                imageCase.getLesionShapeType(),
                lesionData,
                imageCase.getLesionDataJson(),
                findingDtos,
                diagnosisDtos,
                imageCase.getDeletedAt(),
                imageCase.getUpdatedAt()
        );
    }

    private LesionDataDto parseLesionData(String lesionDataJson) {
        try {
            String json = lesionDataJson == null ? "" : lesionDataJson.trim();
            if (!json.contains("cx") || !json.contains("cy") || !json.contains("r")) {
                throw new IllegalArgumentException("Invalid lesion data");
            }
            String trimmed = json.replaceAll("[{}\"]", "");
            String[] parts = trimmed.split(",");
            double cx = 0.5;
            double cy = 0.5;
            double r = 0.2;
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length != 2) continue;
                String key = kv[0].trim();
                double value = Double.parseDouble(kv[1].trim());
                if (key.equalsIgnoreCase("cx")) cx = value;
                if (key.equalsIgnoreCase("cy")) cy = value;
                if (key.equalsIgnoreCase("r")) r = value;
            }
            return new LesionDataDto(LesionShapeType.CIRCLE.name(), cx, cy, r);
        } catch (Exception e) {
            return new LesionDataDto(LesionShapeType.CIRCLE.name(), 0.5, 0.5, 0.2);
        }
    }
}
