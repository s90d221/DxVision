package com.example.dxvision.domain.casefile.service;

import com.example.dxvision.domain.casefile.CaseDiagnosis;
import com.example.dxvision.domain.casefile.CaseFinding;
import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.casefile.dto.CaseOptionDto;
import com.example.dxvision.domain.casefile.dto.CaseListItemResponse;
import com.example.dxvision.domain.casefile.dto.CaseListPageResponse;
import com.example.dxvision.domain.casefile.dto.CaseSearchRequest;
import com.example.dxvision.domain.casefile.dto.DiagnosisOptionDto;
import com.example.dxvision.domain.casefile.dto.FindingOptionDto;
import com.example.dxvision.domain.progress.UserCaseProgress;
import com.example.dxvision.domain.progress.UserCaseStatus;
import com.example.dxvision.domain.repository.UserCaseProgressRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import com.example.dxvision.global.security.CurrentUserProvider;

@Service
public class CaseService {
    private final CaseQueryService caseQueryService;
    private final ImageCaseRepository imageCaseRepository;
    private final UserCaseProgressRepository userCaseProgressRepository;
    private final CurrentUserProvider currentUserProvider;

    public CaseService(CaseQueryService caseQueryService,
                       ImageCaseRepository imageCaseRepository,
                       UserCaseProgressRepository userCaseProgressRepository,
                       CurrentUserProvider currentUserProvider) {
        this.caseQueryService = caseQueryService;
        this.imageCaseRepository = imageCaseRepository;
        this.userCaseProgressRepository = userCaseProgressRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public CaseOptionDto getRandomCase() {
        ImageCase imageCase = caseQueryService.findRandomCase()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No cases available"));

        List<FindingOptionDto> findingOptions = imageCase.getFindings().stream()
                .map(CaseFinding::getFinding)
                .map(this::toFindingDto)
                .sorted(this::compareOptionOrder)
                .toList();

        List<DiagnosisOptionDto> diagnosisOptions = imageCase.getDiagnoses().stream()
                .map(CaseDiagnosis::getDiagnosis)
                .map(this::toDiagnosisDto)
                .sorted(this::compareDiagnosisOrder)
                .toList();

        return new CaseOptionDto(
                imageCase.getId(),
                imageCase.getVersion(),
                imageCase.getTitle(),
                imageCase.getDescription(),
                imageCase.getModality(),
                imageCase.getSpecies(),
                imageCase.getImageUrl(),
                imageCase.getLesionShapeType(),
                findingOptions,
                diagnosisOptions
        );
    }

    @Transactional(readOnly = true)
    public CaseOptionDto getCaseById(Long caseId) {
        ImageCase imageCase = imageCaseRepository.findWithOptionsById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        List<FindingOptionDto> findingOptions = imageCase.getFindings().stream()
                .map(CaseFinding::getFinding)
                .map(this::toFindingDto)
                .sorted(this::compareOptionOrder)
                .toList();

        List<DiagnosisOptionDto> diagnosisOptions = imageCase.getDiagnoses().stream()
                .map(CaseDiagnosis::getDiagnosis)
                .map(this::toDiagnosisDto)
                .sorted(this::compareDiagnosisOrder)
                .toList();

        return new CaseOptionDto(
                imageCase.getId(),
                imageCase.getVersion(),
                imageCase.getTitle(),
                imageCase.getDescription(),
                imageCase.getModality(),
                imageCase.getSpecies(),
                imageCase.getImageUrl(),
                imageCase.getLesionShapeType(),
                findingOptions,
                diagnosisOptions
        );
    }

    @Transactional(readOnly = true)
    public CaseListPageResponse<CaseListItemResponse> searchCases(CaseSearchRequest request) {
        Long userId = currentUserProvider.getCurrentUser().getId();

        Set<Long> includeIds = null;
        Set<Long> excludeIds = null;

        if (request.status() != null) {
            if (request.status().isUnseen()) {
                List<Long> progressedIds = userCaseProgressRepository.findCaseIdsByUserIdAndStatus(userId, null);
                excludeIds = Set.copyOf(progressedIds);
            } else {
                List<Long> idsByStatus = userCaseProgressRepository.findCaseIdsByUserIdAndStatus(userId, request.status());
                includeIds = Set.copyOf(idsByStatus);
                if (includeIds.isEmpty()) {
                    return new CaseListPageResponse<>(
                            List.of(),
                            request.page(),
                            request.size(),
                            0,
                            0
                    );
                }
            }
        }

        var specification = CaseSpecifications.filter(
                request.modality(),
                request.species(),
                request.keyword(),
                includeIds,
                excludeIds
        );

        var page = imageCaseRepository.findAll(specification, request.pageRequest());
        List<Long> caseIds = page.getContent().stream().map(ImageCase::getId).toList();

        Map<Long, UserCaseProgress> progressMap = caseIds.isEmpty()
                ? Map.of()
                : userCaseProgressRepository.findByUserIdAndCaseIds(userId, caseIds).stream()
                .collect(Collectors.toMap(p -> p.getImageCase().getId(), p -> p));

        List<CaseListItemResponse> content = page.getContent().stream()
                .map(ic -> {
                    UserCaseProgress progress = progressMap.get(ic.getId());
                    UserCaseStatus status = UserCaseStatus.normalize(progress == null ? null : progress.getStatus());
                    return new CaseListItemResponse(
                            ic.getId(),
                            ic.getTitle(),
                            ic.getModality(),
                            ic.getSpecies(),
                            ic.getUpdatedAt(),
                            status,
                            progress == null ? null : progress.getLastAttemptAt(),
                            progress == null ? null : progress.getLastScore()
                    );
                })
                .toList();

        return new CaseListPageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private FindingOptionDto toFindingDto(com.example.dxvision.domain.casefile.Finding f) {
        return new FindingOptionDto(
                f.getId(),
                f.getLabel(),
                f.getFolder() != null ? f.getFolder().getId() : null,
                f.getFolder() != null ? f.getFolder().getName() : null,
                f.getOrderIndex(),
                f.getFolder() != null ? f.getFolder().getOrderIndex() : null
        );
    }

    private DiagnosisOptionDto toDiagnosisDto(com.example.dxvision.domain.casefile.Diagnosis d) {
        return new DiagnosisOptionDto(
                d.getId(),
                d.getName(),
                d.getFolder() != null ? d.getFolder().getId() : null,
                d.getFolder() != null ? d.getFolder().getName() : null,
                d.getOrderIndex(),
                d.getFolder() != null ? d.getFolder().getOrderIndex() : null
        );
    }

    private int compareOptionOrder(FindingOptionDto a, FindingOptionDto b) {
        int folderCompare = compareNullable(a.folderOrderIndex(), b.folderOrderIndex());
        if (folderCompare != 0) return folderCompare;
        int orderCompare = compareNullable(a.orderIndex(), b.orderIndex());
        if (orderCompare != 0) return orderCompare;
        return a.label().compareToIgnoreCase(b.label());
    }

    private int compareDiagnosisOrder(DiagnosisOptionDto a, DiagnosisOptionDto b) {
        int folderCompare = compareNullable(a.folderOrderIndex(), b.folderOrderIndex());
        if (folderCompare != 0) return folderCompare;
        int orderCompare = compareNullable(a.orderIndex(), b.orderIndex());
        if (orderCompare != 0) return orderCompare;
        return a.name().compareToIgnoreCase(b.name());
    }

    private int compareNullable(Long a, Long b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return Long.compare(a, b);
    }

    private int compareNullable(Integer a, Integer b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return Integer.compare(a, b);
    }
}
