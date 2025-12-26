package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.ImageCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ImageCaseRepository extends JpaRepository<ImageCase, Long> {

    @EntityGraph(attributePaths = {"findings", "findings.finding", "diagnoses", "diagnoses.diagnosis"})
    Optional<ImageCase> findWithOptionsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"findings", "findings.finding", "diagnoses", "diagnoses.diagnosis"})
    List<ImageCase> findAll();

    Page<ImageCase> findAll(Pageable pageable);

    @Query(value = "SELECT * FROM image_cases WHERE id = :id", nativeQuery = true)
    Optional<ImageCase> findByIdIncludingDeleted(Long id);

    @Query(
            value = "SELECT * FROM image_cases ORDER BY updated_at DESC",
            countQuery = "SELECT count(*) FROM image_cases",
            nativeQuery = true
    )
    Page<ImageCase> findAllIncludingDeleted(Pageable pageable);
}
