package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.DiagnosisAdminRequest;
import com.example.dxvision.domain.admin.dto.DiagnosisAdminResponse;
import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiagnosisAdminService {
    private final DiagnosisRepository diagnosisRepository;

    public DiagnosisAdminService(DiagnosisRepository diagnosisRepository) {
        this.diagnosisRepository = diagnosisRepository;
    }

    @Transactional
    public DiagnosisAdminResponse create(DiagnosisAdminRequest request) {
        Diagnosis diagnosis = new Diagnosis(request.name(), request.description());
        Diagnosis saved = diagnosisRepository.save(diagnosis);
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
        Diagnosis diagnosis = diagnosisRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagnosis not found"));
        diagnosis.update(request.name(), request.description());
        return toResponse(diagnosis);
    }

    @Transactional
    public void delete(Long id) {
        if (!diagnosisRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagnosis not found");
        }
        diagnosisRepository.deleteById(id);
    }

    private DiagnosisAdminResponse toResponse(Diagnosis diagnosis) {
        return new DiagnosisAdminResponse(diagnosis.getId(), diagnosis.getName(), diagnosis.getDescription());
    }
}
