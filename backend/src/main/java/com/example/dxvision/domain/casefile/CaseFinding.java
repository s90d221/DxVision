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
        name = "case_findings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"image_case_id", "finding_id"})
)
@Getter
@NoArgsConstructor
public class CaseFinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_case_id", nullable = false)
    private ImageCase imageCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    @Column(nullable = false)
    private boolean requiredFinding = true;

    public CaseFinding(ImageCase imageCase, Finding finding, boolean requiredFinding) {
        this.imageCase = imageCase;
        this.finding = finding;
        this.requiredFinding = requiredFinding;
    }

    public CaseFinding(Finding finding, boolean requiredFinding) {
        this(null, finding, requiredFinding);
    }

    public void setImageCase(ImageCase imageCase) {
        this.imageCase = imageCase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CaseFinding that = (CaseFinding) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
