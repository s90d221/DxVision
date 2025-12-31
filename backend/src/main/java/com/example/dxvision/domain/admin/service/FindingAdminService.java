package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.FindingAdminRequest;
import com.example.dxvision.domain.admin.dto.FindingAdminResponse;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.repository.CaseFindingRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FindingAdminService {
    private final FindingRepository findingRepository;
    private final CaseFindingRepository caseFindingRepository;

    public FindingAdminService(
            FindingRepository findingRepository,
            CaseFindingRepository caseFindingRepository
    ) {
        this.findingRepository = findingRepository;
        this.caseFindingRepository = caseFindingRepository;
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
        return new FindingAdminResponse(finding.getId(), finding.getLabel(), finding.getDescription());
    }
}
