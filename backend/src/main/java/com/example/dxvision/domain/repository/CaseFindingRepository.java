package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.CaseFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CaseFindingRepository extends JpaRepository<CaseFinding, Long> {

    // 특정 케이스에 속한 소견 연결 전부 삭제
    @Modifying
    @Query("delete from CaseFinding cf where cf.imageCase.id = :imageCaseId")
    void deleteByImageCaseId(Long imageCaseId);

    // 특정 finding이 어떤 케이스에라도 쓰이고 있는지 확인
    boolean existsByFindingId(Long findingId);
}
