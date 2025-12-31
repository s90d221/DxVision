package com.example.dxvision.domain.casefile.controller;

import com.example.dxvision.domain.casefile.dto.OptionFolderTreeResponse;
import com.example.dxvision.domain.casefile.service.OptionFolderQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/options")
public class OptionFolderQueryController {
    private final OptionFolderQueryService optionFolderQueryService;

    public OptionFolderQueryController(OptionFolderQueryService optionFolderQueryService) {
        this.optionFolderQueryService = optionFolderQueryService;
    }

    @GetMapping("/findings/tree")
    public List<OptionFolderTreeResponse> findingTree() {
        return optionFolderQueryService.getFindingTree();
    }

    @GetMapping("/diagnoses/tree")
    public List<OptionFolderTreeResponse> diagnosisTree() {
        return optionFolderQueryService.getDiagnosisTree();
    }
}
