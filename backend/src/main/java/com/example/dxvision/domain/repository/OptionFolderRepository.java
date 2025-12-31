package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.casefile.OptionFolder;
import com.example.dxvision.domain.casefile.OptionFolderType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OptionFolderRepository extends JpaRepository<OptionFolder, Long> {
    List<OptionFolder> findAllByTypeOrderByOrderIndexAsc(OptionFolderType type);

    @Query("select coalesce(max(f.orderIndex), 0) from OptionFolder f where f.type = :type")
    Integer findMaxOrderIndexByType(OptionFolderType type);
}
