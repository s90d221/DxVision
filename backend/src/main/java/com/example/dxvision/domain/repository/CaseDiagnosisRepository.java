package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.CaseDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CaseDiagnosisRepository extends JpaRepository<CaseDiagnosis, Long> {

    @Modifying
    @Query("delete from CaseDiagnosis cd where cd.imageCase.id = :imageCaseId")
    void deleteByImageCaseId(Long imageCaseId);
}
