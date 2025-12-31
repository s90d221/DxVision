package com.example.dxvision.domain.admin.controller;

import com.example.dxvision.domain.casefile.OptionType;
import com.example.dxvision.domain.casefile.dto.OptionFolderReorderRequest;
import com.example.dxvision.domain.casefile.dto.OptionFolderRequest;
import com.example.dxvision.domain.casefile.dto.OptionFolderResponse;
import com.example.dxvision.domain.casefile.service.OptionFolderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/folders")
public class OptionFolderAdminController {
    private final OptionFolderService optionFolderService;

    public OptionFolderAdminController(OptionFolderService optionFolderService) {
        this.optionFolderService = optionFolderService;
    }

    @GetMapping
    public List<OptionFolderResponse> list(@RequestParam OptionType type) {
        return optionFolderService.listFoldersWithItems(type, null);
    }

    @PostMapping
    public OptionFolderResponse create(@Valid @RequestBody OptionFolderRequest request) {
        return optionFolderService.createFolder(request);
    }

    @PutMapping("/{id}")
    public OptionFolderResponse update(@PathVariable Long id, @Valid @RequestBody OptionFolderRequest request) {
        return optionFolderService.updateFolder(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        optionFolderService.deleteFolder(id);
    }

    @PutMapping("/reorder")
    public void reorder(@Valid @RequestBody OptionFolderReorderRequest request) {
        optionFolderService.reorderFolders(request);
    }
}
