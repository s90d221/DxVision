package com.example.dxvision.domain.casefile.dto;

import com.example.dxvision.domain.casefile.OptionType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OptionFolderReorderRequest(
        @NotNull(message = "Type is required")
        OptionType type,
        @NotNull(message = "Orders are required")
        List<FolderOrder> orders
) {
    public record FolderOrder(Long id, Integer sortOrder) {
    }
}
