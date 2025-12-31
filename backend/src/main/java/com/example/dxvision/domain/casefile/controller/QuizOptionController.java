package com.example.dxvision.domain.casefile.controller;

import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.casefile.OptionType;
import com.example.dxvision.domain.casefile.dto.OptionFolderResponse;
import com.example.dxvision.domain.casefile.service.OptionFolderService;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/quiz/options")
public class QuizOptionController {
    private final OptionFolderService optionFolderService;
    private final ImageCaseRepository imageCaseRepository;

    public QuizOptionController(OptionFolderService optionFolderService, ImageCaseRepository imageCaseRepository) {
        this.optionFolderService = optionFolderService;
        this.imageCaseRepository = imageCaseRepository;
    }

    @GetMapping
    public List<OptionFolderResponse> list(@RequestParam OptionType type, @RequestParam(required = false) Long caseId) {
        Set<Long> allowed = null;
        if (caseId != null) {
            ImageCase imageCase = imageCaseRepository.findWithOptionsById(caseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
            if (type == OptionType.FINDING) {
                allowed = imageCase.getFindings().stream()
                        .map(cf -> cf.getFinding().getId())
                        .collect(Collectors.toSet());
            } else {
                allowed = imageCase.getDiagnoses().stream()
                        .map(cd -> cd.getDiagnosis().getId())
                        .collect(Collectors.toSet());
            }
        }
        return optionFolderService.listFoldersWithItems(type, allowed);
    }
}
