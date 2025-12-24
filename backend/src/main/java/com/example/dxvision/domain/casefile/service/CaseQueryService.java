package com.example.dxvision.domain.casefile.service;

import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        int pageBound = (int) Math.min(total, Integer.MAX_VALUE);

        for (int attempt = 0; attempt < 3; attempt++) {
            int randomPageIndex = ThreadLocalRandom.current().nextInt(pageBound);
            Optional<Long> caseId = imageCaseRepository.findAll(
                            PageRequest.of(randomPageIndex, 1, Sort.by("id").ascending()))
                    .stream()
                    .findFirst()
                    .map(ImageCase::getId);
            if (caseId.isPresent()) {
                return imageCaseRepository.findWithOptionsById(caseId.get());
            }
        }

        return imageCaseRepository.findAll(PageRequest.of(0, 1, Sort.by("id").ascending()))
                .stream()
                .findFirst()
                .map(ImageCase::getId)
                .flatMap(imageCaseRepository::findWithOptionsById);
    }
}
