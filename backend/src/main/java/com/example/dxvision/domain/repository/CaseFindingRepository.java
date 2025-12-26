package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.CaseFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CaseFindingRepository extends JpaRepository<CaseFinding, Long> {

    @Modifying
    @Query("delete from CaseFinding cf where cf.imageCase.id = :imageCaseId")
    void deleteByImageCaseId(Long imageCaseId);
}
