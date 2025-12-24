package com.example.dxvision.domain.progress;

import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.attempt.Attempt;
import com.example.dxvision.domain.casefile.ImageCase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_case_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "image_case_id"})
)
@Getter
@NoArgsConstructor
public class UserCaseProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_case_id")
    private ImageCase imageCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserCaseStatus status;

    @Column
    private Long lastAttemptId;

    @Column
    private Double lastScore;

    @Column
    private Instant lastAttemptAt;

    @Column(nullable = false)
    private int correctCount;

    @Column(nullable = false)
    private int wrongCount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public UserCaseProgress(User user, ImageCase imageCase, UserCaseStatus status) {
        this.user = user;
        this.imageCase = imageCase;
        this.status = status;
    }

    public void recordAttempt(UserCaseStatus nextStatus, Attempt attempt, boolean isCorrect) {
        this.status = nextStatus;
        this.lastAttemptId = attempt.getId();
        this.lastScore = attempt.getFinalScore();
        this.lastAttemptAt = attempt.getSubmittedAt();
        if (isCorrect) {
            this.correctCount += 1;
        } else {
            this.wrongCount += 1;
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
