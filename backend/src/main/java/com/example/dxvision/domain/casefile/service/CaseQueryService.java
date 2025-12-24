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
        * MVP-scale random selection using count + random offset to avoid ORDER BY RAND().
        */
    @Transactional(readOnly = true)
    public Optional<ImageCase> findRandomCase() {
        long total = imageCaseRepository.count();
        if (total == 0) {
            return Optional.empty();
        }
        long offset = ThreadLocalRandom.current().nextLong(total);
        Page<ImageCase> page = imageCaseRepository.findAll(PageRequest.of(Math.toIntExact(offset), 1));
        if (page.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(page.getContent().getFirst());
    }
}
