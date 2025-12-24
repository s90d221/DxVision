package com.example.dxvision.domain.attempt;

import com.example.dxvision.domain.casefile.Finding;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attempt_finding_answers")
@Getter
@NoArgsConstructor
public class AttemptFindingAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    public AttemptFindingAnswer(Attempt attempt, Finding finding) {
        this.attempt = attempt;
        this.finding = finding;
    }

    void attachToAttempt(Attempt attempt) {
        this.attempt = attempt;
    }
}
