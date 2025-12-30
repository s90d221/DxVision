package com.example.dxvision.domain.casefile.service;

import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CaseSpecifications {
    private CaseSpecifications() {
    }

    public static Specification<ImageCase> filter(
            String modality,
            String species,
            String keyword,
            Set<Long> includeIds,
            Set<Long> excludeIds
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(modality)) {
                try {
                    Modality modalityEnum = Modality.valueOf(modality.toUpperCase());
                    predicates.add(cb.equal(root.get("modality"), modalityEnum));
                } catch (IllegalArgumentException ignored) {
                    predicates.add(cb.disjunction());
                }
            }

            if (StringUtils.hasText(species)) {
                try {
                    Species speciesEnum = Species.valueOf(species.toUpperCase());
                    predicates.add(cb.equal(root.get("species"), speciesEnum));
                } catch (IllegalArgumentException ignored) {
                    predicates.add(cb.disjunction());
                }
            }

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("title")), pattern),
                                cb.like(cb.lower(root.get("description")), pattern)
                        )
                );
            }

            if (includeIds != null && !includeIds.isEmpty()) {
                predicates.add(root.get("id").in(includeIds));
            }

            if (excludeIds != null && !excludeIds.isEmpty()) {
                predicates.add(cb.not(root.get("id").in(excludeIds)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
