package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.Diagnosis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    // 중복 이름 체크 (대소문자 무시)
    boolean existsByNameIgnoreCase(String name);

    List<Diagnosis> findAllByOrderByOrderIndexAsc();

    List<Diagnosis> findByFolderIdOrderByOrderIndexAsc(Long folderId);

    List<Diagnosis> findByFolderIsNullOrderByOrderIndexAsc();

    boolean existsByFolderId(Long folderId);
}
