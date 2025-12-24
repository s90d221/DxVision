package com.example.dxvision.domain.admin.controller;

import com.example.dxvision.domain.admin.dto.FindingAdminRequest;
import com.example.dxvision.domain.admin.dto.FindingAdminResponse;
import com.example.dxvision.domain.admin.service.FindingAdminService;
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
@RequestMapping("/api/v1/admin/findings")
public class FindingAdminController {
    private final FindingAdminService findingAdminService;

    public FindingAdminController(FindingAdminService findingAdminService) {
        this.findingAdminService = findingAdminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FindingAdminResponse create(@Valid @RequestBody FindingAdminRequest request) {
        return findingAdminService.create(request);
    }

    @GetMapping
    public List<FindingAdminResponse> list() {
        return findingAdminService.list();
    }

    @PutMapping("/{id}")
    public ResponseEntity<FindingAdminResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FindingAdminRequest request
    ) {
        return ResponseEntity.ok(findingAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        findingAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
