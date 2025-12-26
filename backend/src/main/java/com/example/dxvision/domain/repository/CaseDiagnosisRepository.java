package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.CaseDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CaseDiagnosisRepository extends JpaRepository<CaseDiagnosis, Long> {

    // 특정 케이스에 속한 진단 연결 전부 삭제
    @Modifying
    @Query("delete from CaseDiagnosis cd where cd.imageCase.id = :imageCaseId")
    void deleteByImageCaseId(Long imageCaseId);

    // 특정 diagnosis가 어떤 케이스에라도 쓰이고 있는지 확인
    boolean existsByDiagnosisId(Long diagnosisId);
}
