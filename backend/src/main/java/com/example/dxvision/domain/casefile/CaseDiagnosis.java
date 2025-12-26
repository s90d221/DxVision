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
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

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

    public CaseDiagnosis(Diagnosis diagnosis, double weight) {
        this(null, diagnosis, weight);
    }

    public void setImageCase(ImageCase imageCase) {
        this.imageCase = imageCase;
    }

    public void updateWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CaseDiagnosis that = (CaseDiagnosis) o;

        Object thisImageCaseKey = imageCase != null && imageCase.getId() != null ? imageCase.getId() : imageCase;
        Object thatImageCaseKey = that.imageCase != null && that.imageCase.getId() != null ? that.imageCase.getId() : that.imageCase;

        Object thisDiagnosisKey = diagnosis != null && diagnosis.getId() != null ? diagnosis.getId() : diagnosis;
        Object thatDiagnosisKey = that.diagnosis != null && that.diagnosis.getId() != null ? that.diagnosis.getId() : that.diagnosis;

        return Objects.equals(thisImageCaseKey, thatImageCaseKey)
                && Objects.equals(thisDiagnosisKey, thatDiagnosisKey);
    }

    @Override
    public int hashCode() {
        Object imageCaseKey = imageCase != null && imageCase.getId() != null ? imageCase.getId() : imageCase;
        Object diagnosisKey = diagnosis != null && diagnosis.getId() != null ? diagnosis.getId() : diagnosis;
        return Objects.hash(imageCaseKey, diagnosisKey);
    }
}
