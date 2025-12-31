package com.example.dxvision.domain.casefile.dto;

import com.example.dxvision.domain.casefile.OptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OptionFolderRequest(
        @NotNull(message = "Type is required")
        OptionType type,
        @NotBlank(message = "Name is required")
        String name,
        Integer sortOrder
) {
}
