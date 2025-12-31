package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.FindingFolder;
import com.example.dxvision.domain.casefile.OptionType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FindingFolderRepository extends JpaRepository<FindingFolder, Long> {
    boolean existsByFindingId(Long findingId);

    @Query("""
            select ff from FindingFolder ff
            join fetch ff.folder f
            join fetch ff.finding fd
            where f.type = :type
            order by f.sortOrder asc, ff.sortOrder asc, fd.label asc
            """)
    List<FindingFolder> findOrderedByType(OptionType type);

    @Query("""
            select ff from FindingFolder ff
            join fetch ff.folder f
            join fetch ff.finding fd
            where f.type = :type and fd.id in :findingIds
            order by f.sortOrder asc, ff.sortOrder asc, fd.label asc
            """)
    List<FindingFolder> findOrderedByTypeAndFindingIds(OptionType type, Collection<Long> findingIds);

    List<FindingFolder> findByFolderId(Long folderId);

    @Query("select max(ff.sortOrder) from FindingFolder ff where ff.folder.id = :folderId")
    Integer findMaxSortOrderByFolderId(Long folderId);
}
