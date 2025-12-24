package com.example.dxvision.domain.casefile.service;

import com.example.dxvision.domain.casefile.CaseDiagnosis;
import com.example.dxvision.domain.casefile.CaseFinding;
import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.casefile.dto.CaseOptionDto;
import com.example.dxvision.domain.casefile.dto.DiagnosisOptionDto;
import com.example.dxvision.domain.casefile.dto.FindingOptionDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.dxvision.domain.repository.ImageCaseRepository;

@Service
public class CaseService {
    private final CaseQueryService caseQueryService;
    private final ImageCaseRepository imageCaseRepository;

    public CaseService(CaseQueryService caseQueryService,
                       ImageCaseRepository imageCaseRepository) {
        this.caseQueryService = caseQueryService;
        this.imageCaseRepository = imageCaseRepository;
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
}
