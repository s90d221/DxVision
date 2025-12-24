package com.example.dxvision.domain.admin.controller;

import com.example.dxvision.domain.admin.dto.DiagnosisAdminRequest;
import com.example.dxvision.domain.admin.dto.DiagnosisAdminResponse;
import com.example.dxvision.domain.admin.service.DiagnosisAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/diagnoses")
public class DiagnosisAdminController {
    private final DiagnosisAdminService diagnosisAdminService;

    public DiagnosisAdminController(DiagnosisAdminService diagnosisAdminService) {
        this.diagnosisAdminService = diagnosisAdminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisAdminResponse create(@Valid @RequestBody DiagnosisAdminRequest request) {
        return diagnosisAdminService.create(request);
    }

    @GetMapping
    public List<DiagnosisAdminResponse> list() {
        return diagnosisAdminService.list();
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiagnosisAdminResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DiagnosisAdminRequest request
    ) {
        return ResponseEntity.ok(diagnosisAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        diagnosisAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
