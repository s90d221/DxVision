package com.example.dxvision.domain.casefile.dto;

import java.util.List;

public record OptionFolderTreeResponse(
        Long folderId,
        String folderName,
        Integer orderIndex,
        List<OptionItemResponse> items
) {
    public record OptionItemResponse(Long id, String label, Integer orderIndex) {
    }
}
