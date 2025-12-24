package com.example.dxvision.domain.casefile.controller;

import com.example.dxvision.domain.casefile.dto.CaseOptionDto;
import com.example.dxvision.domain.casefile.service.CaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

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
}
