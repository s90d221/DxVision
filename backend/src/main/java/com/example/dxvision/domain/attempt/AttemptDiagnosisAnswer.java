package com.example.dxvision.domain.attempt;

import com.example.dxvision.domain.casefile.Diagnosis;
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
@Table(name = "attempt_diagnosis_answers")
@Getter
@NoArgsConstructor
public class AttemptDiagnosisAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private Diagnosis diagnosis;

    public AttemptDiagnosisAnswer(Attempt attempt, Diagnosis diagnosis) {
        this.attempt = attempt;
        this.diagnosis = diagnosis;
    }

    void attachToAttempt(Attempt attempt) {
        this.attempt = attempt;
    }
}
