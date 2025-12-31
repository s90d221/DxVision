package com.example.dxvision.domain.admin.controller;

import com.example.dxvision.domain.admin.dto.OptionFolderRequest;
import com.example.dxvision.domain.admin.dto.OptionFolderResponse;
import com.example.dxvision.domain.admin.service.OptionFolderAdminService;
import com.example.dxvision.domain.casefile.OptionFolderType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/option-folders")
public class OptionFolderAdminController {
    private final OptionFolderAdminService optionFolderAdminService;

    public OptionFolderAdminController(OptionFolderAdminService optionFolderAdminService) {
        this.optionFolderAdminService = optionFolderAdminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OptionFolderResponse create(@Valid @RequestBody OptionFolderRequest request) {
        return optionFolderAdminService.create(request);
    }

    @GetMapping
    public List<OptionFolderResponse> list(@RequestParam OptionFolderType type) {
        return optionFolderAdminService.list(type);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OptionFolderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody OptionFolderRequest request
    ) {
        return ResponseEntity.ok(optionFolderAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        optionFolderAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
