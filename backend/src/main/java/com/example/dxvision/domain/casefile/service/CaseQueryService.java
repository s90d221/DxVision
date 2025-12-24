package com.example.dxvision.domain.casefile.service;

import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseQueryService {
    private final ImageCaseRepository imageCaseRepository;

    public CaseQueryService(ImageCaseRepository imageCaseRepository) {
        this.imageCaseRepository = imageCaseRepository;
    }

    /**
     * MVP-scale random selection using count + random page index to avoid ORDER BY RAND().
     * If total rows exceed Integer.MAX_VALUE, we cap the page index within int range.
     */
    @Transactional(readOnly = true)
    public Optional<ImageCase> findRandomCase() {
        long total = imageCaseRepository.count();
        if (total == 0) {
            return Optional.empty();
        }
        int maxPageIndex = (int) Math.min(total - 1, Integer.MAX_VALUE);

        for (int attempt = 0; attempt < 3; attempt++) {
            int randomPageIndex = ThreadLocalRandom.current().nextInt(maxPageIndex + 1);
            Page<ImageCase> page = imageCaseRepository.findAll(PageRequest.of(randomPageIndex, 1, org.springframework.data.domain.Sort.by("id").ascending()));
            if (!page.isEmpty()) {
                Long caseId = page.getContent().getFirst().getId();
                return imageCaseRepository.findWithOptionsById(caseId);
            }
        }

        Page<ImageCase> fallback = imageCaseRepository.findAll(PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("id").ascending()));
        if (!fallback.isEmpty()) {
            Long caseId = fallback.getContent().getFirst().getId();
            return imageCaseRepository.findWithOptionsById(caseId);
        }
        return Optional.empty();
    }
}
