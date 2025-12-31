package com.example.dxvision.domain.casefile.dto;

import com.example.dxvision.domain.casefile.OptionType;
import java.util.List;

public record OptionFolderResponse(
        Long id,
        String name,
        OptionType type,
        Integer sortOrder,
        boolean systemDefault,
        List<OptionFolderItemDto> items
) {
}
