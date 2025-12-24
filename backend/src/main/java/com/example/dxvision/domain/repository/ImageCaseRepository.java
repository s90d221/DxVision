package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.ImageCase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageCaseRepository extends JpaRepository<ImageCase, Long> {

    @EntityGraph(attributePaths = {"findings", "findings.finding", "diagnoses", "diagnoses.diagnosis"})
    Optional<ImageCase> findWithOptionsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"findings", "findings.finding", "diagnoses", "diagnoses.diagnosis"})
    List<ImageCase> findAll();
}
