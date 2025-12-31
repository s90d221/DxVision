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
import com.example.dxvision.domain.casefile.dto.OptionFolderResponse;
import com.example.dxvision.domain.casefile.OptionType;
import com.example.dxvision.domain.casefile.service.OptionFolderService;
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
    private final OptionFolderService optionFolderService;

    public CaseService(CaseQueryService caseQueryService,
                       ImageCaseRepository imageCaseRepository,
                       UserCaseProgressRepository userCaseProgressRepository,
                       CurrentUserProvider currentUserProvider,
                       OptionFolderService optionFolderService) {
        this.caseQueryService = caseQueryService;
        this.imageCaseRepository = imageCaseRepository;
        this.userCaseProgressRepository = userCaseProgressRepository;
        this.currentUserProvider = currentUserProvider;
        this.optionFolderService = optionFolderService;
    }

    @Transactional(readOnly = true)
    public CaseOptionDto getRandomCase() {
        ImageCase imageCase = caseQueryService.findRandomCase()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No cases available"));

        return buildCaseOptionDto(imageCase);
    }

    @Transactional(readOnly = true)
    public CaseOptionDto getCaseById(Long caseId) {
        ImageCase imageCase = imageCaseRepository.findWithOptionsById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        return buildCaseOptionDto(imageCase);
    }

    private CaseOptionDto buildCaseOptionDto(ImageCase imageCase) {
        List<FindingOptionDto> findingOptions = imageCase.getFindings().stream()
                .map(CaseFinding::getFinding)
                .map(f -> new FindingOptionDto(f.getId(), f.getLabel()))
                .toList();

        List<DiagnosisOptionDto> diagnosisOptions = imageCase.getDiagnoses().stream()
                .map(CaseDiagnosis::getDiagnosis)
                .map(d -> new DiagnosisOptionDto(d.getId(), d.getName()))
                .toList();

        Set<Long> findingIds = imageCase.getFindings().stream()
                .map(cf -> cf.getFinding().getId())
                .collect(Collectors.toSet());
        Set<Long> diagnosisIds = imageCase.getDiagnoses().stream()
                .map(cd -> cd.getDiagnosis().getId())
                .collect(Collectors.toSet());

        List<OptionFolderResponse> findingFolders = optionFolderService.listFoldersWithItems(
                OptionType.FINDING,
                findingIds
        );
        List<OptionFolderResponse> diagnosisFolders = optionFolderService.listFoldersWithItems(
                OptionType.DIAGNOSIS,
                diagnosisIds
        );

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
                diagnosisOptions,
                findingFolders,
                diagnosisFolders
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
}
