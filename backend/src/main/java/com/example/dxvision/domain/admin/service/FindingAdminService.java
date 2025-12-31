package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.FindingAdminRequest;
import com.example.dxvision.domain.admin.dto.FindingAdminResponse;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.casefile.OptionType;
import com.example.dxvision.domain.casefile.service.OptionFolderService;
import com.example.dxvision.domain.repository.CaseFindingRepository;
import com.example.dxvision.domain.repository.FindingFolderRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FindingAdminService {
    private final FindingRepository findingRepository;
    private final CaseFindingRepository caseFindingRepository;
    private final OptionFolderService optionFolderService;
    private final FindingFolderRepository findingFolderRepository;

    public FindingAdminService(
            FindingRepository findingRepository,
            CaseFindingRepository caseFindingRepository,
            OptionFolderService optionFolderService,
            FindingFolderRepository findingFolderRepository
    ) {
        this.findingRepository = findingRepository;
        this.caseFindingRepository = caseFindingRepository;
        this.optionFolderService = optionFolderService;
        this.findingFolderRepository = findingFolderRepository;
    }

    @Transactional
    public FindingAdminResponse create(FindingAdminRequest request) {
        String label = validateAndNormalizeLabel(request);

        // 중복 방지 (선제적으로 409)
        if (findingRepository.existsByLabelIgnoreCase(label)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finding label already exists");
        }

        Finding finding = new Finding(label, request.description());
        Finding saved = findingRepository.save(finding);
        optionFolderService.syncFindingFolders(saved, request.folderIds());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FindingAdminResponse> list() {
        return findingRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FindingAdminResponse update(Long id, FindingAdminRequest request) {
        String label = validateAndNormalizeLabel(request);

        Finding finding = findingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found"));

        // 라벨 변경 시에만 중복 체크
        if (!finding.getLabel().equalsIgnoreCase(label)
                && findingRepository.existsByLabelIgnoreCase(label)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finding label already exists");
        }

        finding.update(label, request.description());
        optionFolderService.syncFindingFolders(finding, request.folderIds());
        return toResponse(finding);
    }

    @Transactional
    public void delete(Long id) {
        if (!findingRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found");
        }

        // 참조 중이면 삭제 금지 (운영 안전)
        if (caseFindingRepository.existsByFindingId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Finding is referenced by one or more cases"
            );
        }

        findingRepository.deleteById(id);
    }

    private String validateAndNormalizeLabel(FindingAdminRequest request) {
        if (request == null || !StringUtils.hasText(request.label())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding label is required");
        }
        String label = request.label().trim();
        if (label.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding label is required");
        }
        if (label.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding label must be <= 200 chars");
        }
        return label;
    }

    private FindingAdminResponse toResponse(Finding finding) {
        List<Long> folderIds = findingFolderRepository.findOrderedByTypeAndFindingIds(
                        OptionType.FINDING,
                        List.of(finding.getId()))
                .stream()
                .map(mapping -> mapping.getFolder().getId())
                .collect(Collectors.toList());
        return new FindingAdminResponse(finding.getId(), finding.getLabel(), finding.getDescription(), folderIds);
    }
}
