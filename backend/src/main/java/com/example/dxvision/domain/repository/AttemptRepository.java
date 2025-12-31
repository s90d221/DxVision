package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.attempt.Attempt;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    long countByUserId(Long userId);

    @Query("""
            select a from Attempt a
            join fetch a.imageCase ic
            where a.user.id = :userId
            order by a.submittedAt desc
            """)
    List<Attempt> findRecentAttempts(
            @Param("userId") Long userId,
            Pageable pageable
    );

    List<Attempt> findByUserIdAndSubmittedAtBetween(Long userId, Instant start, Instant end);

    @Query("""
            select a from Attempt a
            where a.user.id = :userId
            and a.submittedAt >= :start
            and a.submittedAt < :end
            and a.finalScore >= :threshold
            """)
    List<Attempt> findCorrectAttemptsByUserIdAndSubmittedAtBetween(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("threshold") double threshold
    );
}
