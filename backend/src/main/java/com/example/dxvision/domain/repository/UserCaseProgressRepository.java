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

    @Query("""
            select p from UserCaseProgress p
            join fetch p.imageCase ic
            where p.user.id = :userId
            """)
    List<UserCaseProgress> findByUserIdWithCase(@Param("userId") Long userId);

    @Query("""
            select p from UserCaseProgress p
            join fetch p.imageCase ic
            where p.user.id = :userId and p.imageCase.id in :caseIds
            """)
    List<UserCaseProgress> findByUserIdAndCaseIds(
            @Param("userId") Long userId,
            @Param("caseIds") List<Long> caseIds
    );

    @Query("""
            select p.imageCase.id from UserCaseProgress p
            where p.user.id = :userId and (:status is null or p.status = :status)
            """)
    List<Long> findCaseIdsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") UserCaseStatus status
    );

    @Query("""
            select p.user.id as userId,
                   sum(p.correctCount) as correctAttempts,
                   sum(p.wrongCount) as wrongAttempts,
                   sum(case when p.status = 'CORRECT' then 1 else 0 end) as correctCases,
                   sum(case when p.status = 'WRONG' then 1 else 0 end) as wrongCases,
                   sum(case when p.status = 'REATTEMPT_CORRECT' then 1 else 0 end) as reattemptCorrectCases,
                   max(p.lastAttemptAt) as lastAttemptAt
            from UserCaseProgress p
            where p.user.id in :userIds
            group by p.user.id
            """)
    List<UserProgressAggregate> aggregateByUserIds(@Param("userIds") List<Long> userIds);
}
