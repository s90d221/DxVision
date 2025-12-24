package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.progress.UserCaseProgress;
import com.example.dxvision.domain.progress.UserCaseStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCaseProgressRepository extends JpaRepository<UserCaseProgress, Long> {
    Optional<UserCaseProgress> findByUserIdAndImageCaseId(Long userId, Long imageCaseId);

    long countByUserIdAndStatus(Long userId, UserCaseStatus status);

    @Query("""
            select p from UserCaseProgress p
            join fetch p.imageCase ic
            where p.user.id = :userId and p.status = :status
            order by p.updatedAt desc
            """)
    List<UserCaseProgress> findByUserIdAndStatusWithCase(
            @Param("userId") Long userId,
            @Param("status") UserCaseStatus status
    );
}
