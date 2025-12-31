package com.example.dxvision.domain.admin.dto;

import com.example.dxvision.domain.casefile.OptionFolderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OptionFolderRequest(
        @NotNull(message = "Type is required")
        OptionFolderType type,
        @NotBlank(message = "Name is required")
        String name,
        Integer orderIndex
) {
}
