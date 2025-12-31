package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.DiagnosisAdminRequest;
import com.example.dxvision.domain.admin.dto.DiagnosisAdminResponse;
import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.OptionType;
import com.example.dxvision.domain.casefile.service.OptionFolderService;
import com.example.dxvision.domain.repository.CaseDiagnosisRepository;
import com.example.dxvision.domain.repository.DiagnosisFolderRepository;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiagnosisAdminService {
    private final DiagnosisRepository diagnosisRepository;
    private final CaseDiagnosisRepository caseDiagnosisRepository;
    private final OptionFolderService optionFolderService;
    private final DiagnosisFolderRepository diagnosisFolderRepository;

    public DiagnosisAdminService(
            DiagnosisRepository diagnosisRepository,
            CaseDiagnosisRepository caseDiagnosisRepository,
            OptionFolderService optionFolderService,
            DiagnosisFolderRepository diagnosisFolderRepository
    ) {
        this.diagnosisRepository = diagnosisRepository;
        this.caseDiagnosisRepository = caseDiagnosisRepository;
        this.optionFolderService = optionFolderService;
        this.diagnosisFolderRepository = diagnosisFolderRepository;
    }

    @Transactional
    public DiagnosisAdminResponse create(DiagnosisAdminRequest request) {
        String name = validateAndNormalizeName(request);

        // 중복 방지 (선제적으로 409)
        if (diagnosisRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Diagnosis name already exists");
        }

        Diagnosis diagnosis = new Diagnosis(name, request.description());
        Diagnosis saved = diagnosisRepository.save(diagnosis);
        optionFolderService.syncDiagnosisFolders(saved, request.folderIds());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisAdminResponse> list() {
        return diagnosisRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DiagnosisAdminResponse update(Long id, DiagnosisAdminRequest request) {
        String name = validateAndNormalizeName(request);

        Diagnosis diagnosis = diagnosisRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagnosis not found"));

        // 이름 변경 시에만 중복 체크
        if (!diagnosis.getName().equalsIgnoreCase(name)
                && diagnosisRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Diagnosis name already exists");
        }

        diagnosis.update(name, request.description());
        optionFolderService.syncDiagnosisFolders(diagnosis, request.folderIds());
        return toResponse(diagnosis);
    }

    @Transactional
    public void delete(Long id) {
        if (!diagnosisRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagnosis not found");
        }

        // 참조 중이면 삭제 금지 (운영 안전)
        if (caseDiagnosisRepository.existsByDiagnosisId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Diagnosis is referenced by one or more cases"
            );
        }

        diagnosisRepository.deleteById(id);
    }

    private String validateAndNormalizeName(DiagnosisAdminRequest request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnosis name is required");
        }
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnosis name is required");
        }
        if (name.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnosis name must be <= 200 chars");
        }
        return name;
    }

    private DiagnosisAdminResponse toResponse(Diagnosis diagnosis) {
        List<Long> folderIds = diagnosisFolderRepository.findOrderedByTypeAndDiagnosisIds(
                        OptionType.DIAGNOSIS,
                        List.of(diagnosis.getId()))
                .stream()
                .map(mapping -> mapping.getFolder().getId())
                .collect(Collectors.toList());
        return new DiagnosisAdminResponse(diagnosis.getId(), diagnosis.getName(), diagnosis.getDescription(), folderIds);
    }
}
