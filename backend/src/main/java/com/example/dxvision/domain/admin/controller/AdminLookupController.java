package com.example.dxvision.domain.admin.controller;

import com.example.dxvision.domain.admin.dto.LookupDiagnosisDto;
import com.example.dxvision.domain.admin.dto.LookupFindingDto;
import com.example.dxvision.domain.admin.dto.LookupResponse;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminLookupController {
    private final FindingRepository findingRepository;
    private final DiagnosisRepository diagnosisRepository;

    public AdminLookupController(FindingRepository findingRepository, DiagnosisRepository diagnosisRepository) {
        this.findingRepository = findingRepository;
        this.diagnosisRepository = diagnosisRepository;
    }

    @GetMapping("/lookups")
    public LookupResponse lookups() {
        List<LookupFindingDto> findings = findingRepository.findAll().stream()
                .map(f -> new LookupFindingDto(f.getId(), f.getLabel()))
                .toList();
        List<LookupDiagnosisDto> diagnoses = diagnosisRepository.findAll().stream()
                .map(d -> new LookupDiagnosisDto(d.getId(), d.getName()))
                .toList();
        return new LookupResponse(findings, diagnoses);
    }
}
