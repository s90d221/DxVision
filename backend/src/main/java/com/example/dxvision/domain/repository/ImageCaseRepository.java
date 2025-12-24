package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.ImageCase;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageCaseRepository extends JpaRepository<ImageCase, Long> {
    @EntityGraph(attributePaths = {"findings", "findings.finding", "diagnoses", "diagnoses.diagnosis"})
    Optional<ImageCase> findWithOptionsById(Long id);

    @EntityGraph(attributePaths = {"findings", "findings.finding", "diagnoses", "diagnoses.diagnosis"})
    List<ImageCase> findAllWithOptions();
}
