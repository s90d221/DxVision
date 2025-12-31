package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.OptionFolderRequest;
import com.example.dxvision.domain.admin.dto.OptionFolderResponse;
import com.example.dxvision.domain.casefile.OptionFolder;
import com.example.dxvision.domain.casefile.OptionFolderType;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.OptionFolderRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OptionFolderAdminService {
    private final OptionFolderRepository folderRepository;
    private final FindingRepository findingRepository;
    private final DiagnosisRepository diagnosisRepository;

    public OptionFolderAdminService(
            OptionFolderRepository folderRepository,
            FindingRepository findingRepository,
            DiagnosisRepository diagnosisRepository
    ) {
        this.folderRepository = folderRepository;
        this.findingRepository = findingRepository;
        this.diagnosisRepository = diagnosisRepository;
    }

    @Transactional
    public OptionFolderResponse create(OptionFolderRequest request) {
        int orderIndex = resolveOrderIndex(request.type(), request.orderIndex());
        OptionFolder folder = new OptionFolder(request.type(), request.name(), orderIndex);
        return toResponse(folderRepository.save(folder));
    }

    @Transactional(readOnly = true)
    public List<OptionFolderResponse> list(OptionFolderType type) {
        return folderRepository.findAllByTypeOrderByOrderIndexAsc(type).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OptionFolderResponse update(Long id, OptionFolderRequest request) {
        OptionFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        if (folder.getType() != request.type()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder type cannot be changed");
        }
        folder.update(request.name(), request.orderIndex());
        return toResponse(folder);
    }

    @Transactional
    public void delete(Long id) {
        OptionFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        boolean hasFinding = findingRepository.existsByFolderId(folder.getId());
        boolean hasDiagnosis = diagnosisRepository.existsByFolderId(folder.getId());
        if (hasFinding || hasDiagnosis) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Folder has assigned options");
        }
        folderRepository.delete(folder);
    }

    private int resolveOrderIndex(OptionFolderType type, Integer requested) {
        if (requested != null) return requested;
        Integer max = folderRepository.findMaxOrderIndexByType(type);
        return max == null ? 0 : max + 1;
    }

    private OptionFolderResponse toResponse(OptionFolder folder) {
        return new OptionFolderResponse(folder.getId(), folder.getType(), folder.getName(), folder.getOrderIndex());
    }
}
