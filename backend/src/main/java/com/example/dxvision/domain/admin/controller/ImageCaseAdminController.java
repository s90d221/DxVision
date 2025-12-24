package com.example.dxvision.domain.admin.controller;

import com.example.dxvision.domain.admin.dto.AdminCaseRequest;
import com.example.dxvision.domain.admin.dto.AdminCaseResponse;
import com.example.dxvision.domain.admin.service.AdminCaseService;
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
@RequestMapping("/api/v1/admin/cases")
public class ImageCaseAdminController {
    private final AdminCaseService adminCaseService;

    public ImageCaseAdminController(AdminCaseService adminCaseService) {
        this.adminCaseService = adminCaseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCaseResponse create(@Valid @RequestBody AdminCaseRequest request) {
        return adminCaseService.createCase(request);
    }

    @PutMapping("/{caseId}")
    public ResponseEntity<AdminCaseResponse> update(
            @PathVariable Long caseId,
            @Valid @RequestBody AdminCaseRequest request
    ) {
        return ResponseEntity.ok(adminCaseService.updateCase(caseId, request));
    }

    @GetMapping
    public List<AdminCaseResponse> list() {
        return adminCaseService.listCases();
    }

    @GetMapping("/{caseId}")
    public AdminCaseResponse get(@PathVariable Long caseId) {
        return adminCaseService.getCase(caseId);
    }

    @DeleteMapping("/{caseId}")
    public ResponseEntity<Void> delete(@PathVariable Long caseId) {
        adminCaseService.deleteCase(caseId);
        return ResponseEntity.noContent().build();
    }
}
