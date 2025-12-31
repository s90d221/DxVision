package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    // 중복 라벨 체크 (대소문자 무시)
    boolean existsByLabelIgnoreCase(String label);
}
