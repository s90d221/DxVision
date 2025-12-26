package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    // 중복 이름 체크 (대소문자 무시)
    boolean existsByNameIgnoreCase(String name);
}
