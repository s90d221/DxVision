package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.DiagnosisAdminRequest;
import com.example.dxvision.domain.admin.dto.DiagnosisAdminResponse;
import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.OptionFolder;
import com.example.dxvision.domain.casefile.OptionFolderType;
import com.example.dxvision.domain.repository.CaseDiagnosisRepository;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.OptionFolderRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiagnosisAdminService {
    private final DiagnosisRepository diagnosisRepository;
    private final CaseDiagnosisRepository caseDiagnosisRepository;
    private final OptionFolderRepository optionFolderRepository;

    public DiagnosisAdminService(
            DiagnosisRepository diagnosisRepository,
            CaseDiagnosisRepository caseDiagnosisRepository,
            OptionFolderRepository optionFolderRepository
    ) {
        this.diagnosisRepository = diagnosisRepository;
        this.caseDiagnosisRepository = caseDiagnosisRepository;
        this.optionFolderRepository = optionFolderRepository;
    }

    @Transactional
    public DiagnosisAdminResponse create(DiagnosisAdminRequest request) {
        String name = validateAndNormalizeName(request);

        // 중복 방지 (선제적으로 409)
        if (diagnosisRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Diagnosis name already exists");
        }

        Diagnosis diagnosis = new Diagnosis(name, request.description());
        OptionFolder folder = resolveFolder(request.folderId(), OptionFolderType.DIAGNOSIS);
        diagnosis.assignFolder(folder);
        diagnosis.updateOrderIndex(resolveOrderIndex(folder, request.orderIndex()));
        Diagnosis saved = diagnosisRepository.save(diagnosis);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisAdminResponse> list() {
        return diagnosisRepository.findAllByOrderByOrderIndexAsc().stream()
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
        OptionFolder folder = resolveFolder(request.folderId(), OptionFolderType.DIAGNOSIS);
        if (!Objects.equals(folder, diagnosis.getFolder())) {
            diagnosis.assignFolder(folder);
        }
        diagnosis.updateOrderIndex(resolveOrderIndex(folder, request.orderIndex(), diagnosis.getOrderIndex()));
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
        return new DiagnosisAdminResponse(
                diagnosis.getId(),
                diagnosis.getName(),
                diagnosis.getDescription(),
                diagnosis.getFolder() != null ? diagnosis.getFolder().getId() : null,
                diagnosis.getFolder() != null ? diagnosis.getFolder().getName() : null,
                diagnosis.getOrderIndex()
        );
    }

    private OptionFolder resolveFolder(Long folderId, OptionFolderType type) {
        if (folderId == null) return null;
        return optionFolderRepository.findById(folderId)
                .map(folder -> {
                    if (folder.getType() != type) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder type mismatch");
                    }
                    return folder;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
    }

    private int resolveOrderIndex(OptionFolder folder, Integer requested) {
        return resolveOrderIndex(folder, requested, null);
    }

    private int resolveOrderIndex(OptionFolder folder, Integer requested, Integer current) {
        if (requested != null) {
            return requested;
        }
        if (current != null) {
            return current;
        }
        List<Diagnosis> targets = (folder == null)
                ? diagnosisRepository.findByFolderIsNullOrderByOrderIndexAsc()
                : diagnosisRepository.findByFolderIdOrderByOrderIndexAsc(folder.getId());
        if (targets.isEmpty()) {
            return 0;
        }
        return targets.get(targets.size() - 1).getOrderIndex() + 1;
    }
}
