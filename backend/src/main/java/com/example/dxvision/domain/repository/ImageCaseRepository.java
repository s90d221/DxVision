package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.ImageCase;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ImageCaseRepository extends JpaRepository<ImageCase, Long> {
    /**
     * MySQL-friendly random fetch using RAND() for MVP scale.
     */
    @Query(value = "SELECT * FROM image_cases ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<ImageCase> findRandomCase();
}
