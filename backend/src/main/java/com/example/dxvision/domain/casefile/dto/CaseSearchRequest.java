package com.example.dxvision.domain.casefile.dto;

import com.example.dxvision.domain.progress.UserCaseStatus;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public record CaseSearchRequest(
        String modality,
        String species,
        UserCaseStatus status,
        String keyword,
        int page,
        int size,
        Sort sort
) {
    public static CaseSearchRequest of(
            Optional<String> modality,
            Optional<String> species,
            Optional<UserCaseStatus> status,
            Optional<String> keyword,
            int page,
            int size,
            Sort sort
    ) {
        return new CaseSearchRequest(
                modality.map(String::trim).filter(s -> !s.isEmpty()).orElse(null),
                species.map(String::trim).filter(s -> !s.isEmpty()).orElse(null),
                status.orElse(null),
                keyword.map(String::trim).filter(s -> !s.isEmpty()).orElse(null),
                Math.max(page, 0),
                Math.max(size, 1),
                sort
        );
    }

    public PageRequest pageRequest() {
        return PageRequest.of(page, size, sort);
    }
}
