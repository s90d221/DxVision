package com.example.dxvision.domain.casefile.controller;

import com.example.dxvision.domain.casefile.dto.CaseListItemResponse;
import com.example.dxvision.domain.casefile.dto.CaseListPageResponse;
import com.example.dxvision.domain.casefile.dto.CaseOptionDto;
import com.example.dxvision.domain.casefile.dto.CaseSearchRequest;
import com.example.dxvision.domain.casefile.service.CaseService;
import com.example.dxvision.domain.progress.UserCaseStatus;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {
    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping("/random")
    public CaseOptionDto getRandomCase() {
        return caseService.getRandomCase();
    }

    @GetMapping("/{caseId}")
    public CaseOptionDto getCaseById(@PathVariable Long caseId) {
        return caseService.getCaseById(caseId);
    }

    @GetMapping
    public CaseListPageResponse<CaseListItemResponse> listCases(
            @RequestParam Optional<String> modality,
            @RequestParam Optional<String> species,
            @RequestParam Optional<String> status,
            @RequestParam Optional<String> keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Optional<UserCaseStatus> statusEnum = status.flatMap(this::parseStatus);

        CaseSearchRequest request = CaseSearchRequest.of(
                modality,
                species,
                statusEnum,
                keyword,
                page,
                size,
                sortSpec
        );

        return caseService.searchCases(request);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            Sort.Direction direction = Sort.Direction.fromOptionalString(parts[1].trim()).orElse(Sort.Direction.DESC);
            return Sort.by(direction, parts[0].trim());
        }
        return Sort.by(Sort.Direction.DESC, sort.trim());
    }

    private Optional<UserCaseStatus> parseStatus(String status) {
        try {
            return Optional.of(UserCaseStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
