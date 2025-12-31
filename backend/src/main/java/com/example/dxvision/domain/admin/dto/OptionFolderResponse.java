package com.example.dxvision.domain.admin.dto;

import com.example.dxvision.domain.casefile.OptionFolderType;

public record OptionFolderResponse(
        Long id,
        OptionFolderType type,
        String name,
        Integer orderIndex
) {
}
