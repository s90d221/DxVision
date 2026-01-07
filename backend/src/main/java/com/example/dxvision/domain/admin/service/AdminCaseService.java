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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private final ObjectMapper objectMapper;

    public AdminCaseService(
            ImageCaseRepository imageCaseRepository,
            FindingRepository findingRepository,
            DiagnosisRepository diagnosisRepository,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper
    ) {
        this.imageCaseRepository = imageCaseRepository;
        this.findingRepository = findingRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AdminCaseResponse createCase(AdminCaseUpsertRequest request) {
        validateRequest(request, true);

        String lesionDataJson = buildLesionDataJson(request.lesionData());
        LesionShapeType shapeType = resolveShapeType(request.lesionData());

        ImageCase imageCase = new ImageCase(
                request.title(),
                request.description(),
                request.modality(),
                request.species(),
                request.imageUrl(),
                shapeType,
                lesionDataJson
        );

        // ImageCase.replaceFindings/Diagnoses 는 diff 방식이어야 안전함(유니크 충돌 방지)
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
        LesionShapeType shapeType = resolveShapeType(request.lesionData());

        String previousImageUrl = imageCase.getImageUrl();
        String nextImageUrl = StringUtils.hasText(request.imageUrl()) ? request.imageUrl() : previousImageUrl;

        boolean metadataChanged =
                !Objects.equals(imageCase.getTitle(), request.title())
                        || !Objects.equals(imageCase.getDescription(), request.description())
                        || imageCase.getModality() != request.modality()
                        || imageCase.getSpecies() != request.species()
                        || !Objects.equals(previousImageUrl, nextImageUrl);

        // 기존 정답 구성(필수 finding id set)
        Set<Long> previousRequired = imageCase.getFindings().stream()
                .filter(CaseFinding::isRequiredFinding)
                .map(cf -> cf.getFinding().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        // 기존 진단 가중치(이미 저장된 값 기준)
        Map<Long, Double> previousWeights = imageCase.getDiagnoses().stream()
                .collect(HashMap::new,
                        (map, cd) -> map.put(cd.getDiagnosis().getId(), cd.getWeight()),
                        HashMap::putAll);

        boolean lesionChanged = !Objects.equals(imageCase.getLesionDataJson(), lesionDataJson);

        imageCase.updateMetadata(
                request.title(),
                request.description(),
                request.modality(),
                request.species(),
                nextImageUrl,
                shapeType,
                lesionDataJson
        );

        // 관계 컬렉션 업데이트
        imageCase.replaceFindings(applyFindingConfig(request.findings()));
        imageCase.replaceDiagnoses(applyDiagnosisConfig(request.diagnoses()));

        // 새 요청 기준 비교값(필수 finding id set / 정규화 weight map)
        Set<Long> requiredIds = toOrderedSet(
                request.findings() == null
                        ? Collections.emptyList()
                        : request.findings().stream()
                        .filter(AdminFindingSelection::required)
                        .map(AdminFindingSelection::findingId)
                        .toList()
        );

        Map<Long, Double> newWeights = toWeightMap(request.diagnoses());

        boolean correctnessChanged =
                lesionChanged
                        || !previousRequired.equals(requiredIds)
                        || !previousWeights.equals(newWeights);

        if (metadataChanged || correctnessChanged) {
            imageCase.incrementVersion();
        }

        // 이미지 교체된 경우 이전 파일 삭제
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
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is required");
        }
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
        LesionShapeType shapeType = resolveShapeType(lesionData);
        if (shapeType == LesionShapeType.RECT) {
            if (lesionData.x() == null || lesionData.y() == null || lesionData.w() == null || lesionData.h() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rectangle lesion requires x, y, w, h");
            }
            if (lesionData.x() < 0 || lesionData.x() > 1 || lesionData.y() < 0 || lesionData.y() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesion coordinates must be between 0 and 1");
            }
            if (lesionData.w() <= 0 || lesionData.h() <= 0 || lesionData.w() > 1 || lesionData.h() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rectangle width/height must be between 0 and 1");
            }
            if (lesionData.x() + lesionData.w() > 1 || lesionData.y() + lesionData.h() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rectangle must fit within the image");
            }
        } else { // CIRCLE (default) or legacy
            if (lesionData.cx() == null || lesionData.cy() == null || lesionData.r() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesion coordinates are required");
            }
            if (lesionData.cx() < 0 || lesionData.cx() > 1 || lesionData.cy() < 0 || lesionData.cy() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesion coordinates must be between 0 and 1");
            }
            if (lesionData.r() <= 0 || lesionData.r() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Radius must be between 0 and 1");
            }
        }
    }

    private void validateFindings(List<AdminFindingSelection> selections) {
        if (selections == null) {
            return;
        }

        // 중복 방지 + 존재 검증(요청 자체가 안정적이어야 함)
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
        LesionShapeType shapeType = resolveShapeType(lesionData);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", shapeType.name());
        if (shapeType == LesionShapeType.RECT) {
            node.put("x", lesionData.x());
            node.put("y", lesionData.y());
            node.put("w", lesionData.w());
            node.put("h", lesionData.h());
        } else {
            node.put("cx", lesionData.cx());
            node.put("cy", lesionData.cy());
            node.put("r", lesionData.r());
        }
        return node.toString();
    }

    /**
     * NOTE:
     * - IN 조회(findAllById)는 반환 순서가 보장되지 않음.
     * - 그래서 selectionMap으로 required를 찾는 방식은 OK지만,
     *   "요청 순서 유지"가 필요하면 selectionMap 순서로 재정렬해서 생성한다.
     */
    private List<CaseFinding> applyFindingConfig(List<AdminFindingSelection> selections) {
        if (selections == null || selections.isEmpty()) {
            return Collections.emptyList();
        }

        // 요청 중복 방지 + 입력 순서 유지
        Map<Long, AdminFindingSelection> selectionMap = new LinkedHashMap<>();
        for (AdminFindingSelection selection : selections) {
            if (selection == null || selection.findingId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding id is required");
            }
            if (selectionMap.put(selection.findingId(), selection) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate finding selection is not allowed");
            }
        }

        // 실제 엔티티 로드
        List<Finding> findings = findingRepository.findAllById(selectionMap.keySet());
        if (findings.size() != selectionMap.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more findings not found");
        }

        // id -> entity map 구성 후, 요청 순서대로 생성(재현성 확보)
        Map<Long, Finding> findingById = new HashMap<>();
        for (Finding f : findings) {
            findingById.put(f.getId(), f);
        }

        List<CaseFinding> newFindings = new ArrayList<>(selectionMap.size());
        for (Map.Entry<Long, AdminFindingSelection> entry : selectionMap.entrySet()) {
            Long findingId = entry.getKey();
            AdminFindingSelection selection = entry.getValue();

            Finding finding = findingById.get(findingId);
            if (finding == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more findings not found");
            }

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

        Map<Long, Diagnosis> diagnosisById = new HashMap<>();
        for (Diagnosis d : diagnoses) {
            diagnosisById.put(d.getId(), d);
        }

        // weightMap은 LinkedHashMap이라 “요청 입력 순서” 기반 유지됨
        List<CaseDiagnosis> newDiagnoses = new ArrayList<>(weightMap.size());
        for (Map.Entry<Long, Double> entry : weightMap.entrySet()) {
            Long diagnosisId = entry.getKey();
            Double weight = entry.getValue();

            Diagnosis diagnosis = diagnosisById.get(diagnosisId);
            if (diagnosis == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more diagnoses not found");
            }

            newDiagnoses.add(new CaseDiagnosis(diagnosis, weight));
        }

        return newDiagnoses;
    }

    /**
     * weight 정규화는 "영속 저장 값"이므로,
     * - 비교도 정규화된 값으로 하고
     * - 저장도 정규화된 값으로 저장하는 것이 일관성 있음.
     */
    private Map<Long, Double> toWeightMap(List<AdminDiagnosisWeight> diagnosisWeights) {
        if (diagnosisWeights == null) {
            return Collections.emptyMap();
        }

        Map<Long, Double> weightMap = new LinkedHashMap<>();
        double total = 0.0;

        for (AdminDiagnosisWeight req : diagnosisWeights) {
            if (req == null || req.diagnosisId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnosis id is required");
            }
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

    private LesionShapeType resolveShapeType(LesionDataDto lesionData) {
        String type = lesionData != null ? lesionData.type() : null;
        if (!StringUtils.hasText(type)) {
            return LesionShapeType.CIRCLE;
        }
        try {
            return LesionShapeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return LesionShapeType.CIRCLE;
        }
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
            JsonNode node = objectMapper.readTree(lesionDataJson);
            LesionShapeType type = resolveShapeType(new LesionDataDto(
                    node.path("type").asText(),
                    node.path("cx").isMissingNode() ? null : node.path("cx").asDouble(),
                    node.path("cy").isMissingNode() ? null : node.path("cy").asDouble(),
                    node.path("r").isMissingNode() ? null : node.path("r").asDouble(),
                    node.path("x").isMissingNode() ? null : node.path("x").asDouble(),
                    node.path("y").isMissingNode() ? null : node.path("y").asDouble(),
                    node.path("w").isMissingNode() ? null : node.path("w").asDouble(),
                    node.path("h").isMissingNode() ? null : node.path("h").asDouble()
            ));

            if (type == LesionShapeType.RECT) {
                return new LesionDataDto(
                        type.name(),
                        null,
                        null,
                        null,
                        node.path("x").asDouble(),
                        node.path("y").asDouble(),
                        node.path("w").asDouble(),
                        node.path("h").asDouble()
                );
            }

            double cx = node.path("cx").asDouble(0.5);
            double cy = node.path("cy").asDouble(0.5);
            double r = node.path("r").asDouble(0.2);
            return new LesionDataDto(type.name(), cx, cy, r, null, null, null, null);
        } catch (Exception e) {
            return new LesionDataDto(LesionShapeType.CIRCLE.name(), 0.5, 0.5, 0.2, null, null, null, null);
        }
    }
}
