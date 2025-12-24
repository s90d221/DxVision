package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.FindingAdminRequest;
import com.example.dxvision.domain.admin.dto.FindingAdminResponse;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.repository.FindingRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FindingAdminService {
    private final FindingRepository findingRepository;

    public FindingAdminService(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    @Transactional
    public FindingAdminResponse create(FindingAdminRequest request) {
        Finding finding = new Finding(request.label(), request.description());
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
        Finding finding = findingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found"));
        finding.update(request.label(), request.description());
        return toResponse(finding);
    }

    @Transactional
    public void delete(Long id) {
        if (!findingRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found");
        }
        findingRepository.deleteById(id);
    }

    private FindingAdminResponse toResponse(Finding finding) {
        return new FindingAdminResponse(finding.getId(), finding.getLabel(), finding.getDescription());
    }
}
