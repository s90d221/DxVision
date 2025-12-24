package com.example.dxvision.domain.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "case_diagnoses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"image_case_id", "diagnosis_id"})
)
@Getter
@NoArgsConstructor
public class CaseDiagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_case_id", nullable = false)
    private ImageCase imageCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private Diagnosis diagnosis;

    @Column(nullable = false)
    private double weight;

    public CaseDiagnosis(ImageCase imageCase, Diagnosis diagnosis, double weight) {
        this.imageCase = imageCase;
        this.diagnosis = diagnosis;
        this.weight = weight;
    }
}
