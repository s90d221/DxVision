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
                .map(f -> new FindingOptionDto(f.getId(), f.getLabel()))
                .toList();

        List<DiagnosisOptionDto> diagnosisOptions = imageCase.getDiagnoses().stream()
                .map(CaseDiagnosis::getDiagnosis)
                .map(d -> new DiagnosisOptionDto(d.getId(), d.getName()))
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
                .map(f -> new FindingOptionDto(f.getId(), f.getLabel()))
                .toList();

        List<DiagnosisOptionDto> diagnosisOptions = imageCase.getDiagnoses().stream()
                .map(CaseDiagnosis::getDiagnosis)
                .map(d -> new DiagnosisOptionDto(d.getId(), d.getName()))
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
            if (request.status() == UserCaseStatus.UNATTEMPTED) {
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

        Map<Long, UserCaseStatus> statusMap = Map.of();
        Map<Long, java.time.Instant> lastAttemptMap = Map.of();
        if (!caseIds.isEmpty()) {
            var progresses = userCaseProgressRepository.findByUserIdAndCaseIds(userId, caseIds);
            statusMap = progresses.stream()
                    .collect(Collectors.toMap(p -> p.getImageCase().getId(), UserCaseProgress::getStatus));
            lastAttemptMap = progresses.stream()
                    .filter(p -> p.getLastAttemptAt() != null)
                    .collect(Collectors.toMap(p -> p.getImageCase().getId(), UserCaseProgress::getLastAttemptAt));
        }

        List<CaseListItemResponse> content = page.getContent().stream()
                .map(ic -> new CaseListItemResponse(
                        ic.getId(),
                        ic.getTitle(),
                        ic.getModality(),
                        ic.getSpecies(),
                        ic.getUpdatedAt(),
                        statusMap.getOrDefault(ic.getId(), UserCaseStatus.UNATTEMPTED),
                        lastAttemptMap.get(ic.getId())
                ))
                .toList();

        return new CaseListPageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
