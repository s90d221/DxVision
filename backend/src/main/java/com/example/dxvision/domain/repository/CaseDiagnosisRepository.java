package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.CaseDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseDiagnosisRepository extends JpaRepository<CaseDiagnosis, Long> {
}
