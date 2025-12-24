package com.example.dxvision.domain.attempt;

import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.casefile.ImageCase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attempts")
@Getter
@NoArgsConstructor
public class Attempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_case_id", nullable = false)
    private ImageCase imageCase;

    /**
     * Snapshot of ImageCase.version at submission time.
     */
    @Column(nullable = false)
    private Long caseVersion;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private double findingsScore;

    @Column(nullable = false)
    private double locationScore;

    @Column(nullable = false)
    private double diagnosisScore;

    @Column(nullable = false)
    private double finalScore;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @OneToOne(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AttemptLocationAnswer locationAnswer;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AttemptFindingAnswer> findingAnswers = new ArrayList<>();

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AttemptDiagnosisAnswer> diagnosisAnswers = new ArrayList<>();

    public Attempt(User user, ImageCase imageCase, Instant submittedAt) {
        this.user = user;
        this.imageCase = imageCase;
        this.caseVersion = imageCase.getVersion();
        this.submittedAt = submittedAt;
    }

    public void recordScores(
            double findingsScore,
            double locationScore,
            double diagnosisScore,
            double finalScore,
            String explanation
    ) {
        this.findingsScore = findingsScore;
        this.locationScore = locationScore;
        this.diagnosisScore = diagnosisScore;
        this.finalScore = finalScore;
        this.explanation = explanation;
    }

    public void attachLocationAnswer(AttemptLocationAnswer locationAnswer) {
        locationAnswer.attachToAttempt(this);
        this.locationAnswer = locationAnswer;
    }

    public void addFindingAnswer(AttemptFindingAnswer answer) {
        answer.attachToAttempt(this);
        this.findingAnswers.add(answer);
    }

    public void addDiagnosisAnswer(AttemptDiagnosisAnswer answer) {
        answer.attachToAttempt(this);
        this.diagnosisAnswers.add(answer);
    }
}
