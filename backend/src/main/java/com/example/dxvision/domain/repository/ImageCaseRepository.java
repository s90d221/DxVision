package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.ImageCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageCaseRepository extends JpaRepository<ImageCase, Long> {
}
