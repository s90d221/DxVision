package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
}
