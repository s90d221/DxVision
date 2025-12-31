package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.OptionFolder;
import com.example.dxvision.domain.casefile.OptionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OptionFolderRepository extends JpaRepository<OptionFolder, Long> {
    List<OptionFolder> findAllByTypeOrderBySortOrderAsc(OptionType type);

    @Query("select max(f.sortOrder) from OptionFolder f where f.type = :type")
    Integer findMaxSortOrderByType(OptionType type);

    Optional<OptionFolder> findByTypeAndSystemDefaultTrue(OptionType type);
}
