package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.DiagnosisFolder;
import com.example.dxvision.domain.casefile.OptionType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DiagnosisFolderRepository extends JpaRepository<DiagnosisFolder, Long> {
    boolean existsByDiagnosisId(Long diagnosisId);

    @Query("""
            select df from DiagnosisFolder df
            join fetch df.folder f
            join fetch df.diagnosis d
            where f.type = :type
            order by f.sortOrder asc, df.sortOrder asc, d.name asc
            """)
    List<DiagnosisFolder> findOrderedByType(OptionType type);

    @Query("""
            select df from DiagnosisFolder df
            join fetch df.folder f
            join fetch df.diagnosis d
            where f.type = :type and d.id in :diagnosisIds
            order by f.sortOrder asc, df.sortOrder asc, d.name asc
            """)
    List<DiagnosisFolder> findOrderedByTypeAndDiagnosisIds(OptionType type, Collection<Long> diagnosisIds);

    List<DiagnosisFolder> findByFolderId(Long folderId);

    @Query("select max(df.sortOrder) from DiagnosisFolder df where df.folder.id = :folderId")
    Integer findMaxSortOrderByFolderId(Long folderId);
}
