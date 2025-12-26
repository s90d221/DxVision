package com.example.dxvision.domain.casefile;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "image_cases")
@SQLDelete(sql = "UPDATE image_cases SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@NoArgsConstructor
public class ImageCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Domain version for answer drift prevention (not JPA optimistic locking).
     */
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Modality modality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Species species;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    /**
     * MVP location scoring uses CIRCLE; shape type allows future extensibility.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LesionShapeType lesionShapeType;

    /**
     * MVP schema (normalized coordinates 0..1):
     * {"type":"CIRCLE","cx":0.5,"cy":0.5,"r":0.1}
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String lesionDataJson;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant deletedAt;

    // ✅ List → Set 으로 변경 (MultipleBagFetchException 해결)
    @OneToMany(mappedBy = "imageCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CaseFinding> findings = new HashSet<>();

    @OneToMany(mappedBy = "imageCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CaseDiagnosis> diagnoses = new HashSet<>();

    public ImageCase(
            String title,
            String description,
            Modality modality,
            Species species,
            String imageUrl,
            LesionShapeType lesionShapeType,
            String lesionDataJson
    ) {
        this.title = title;
        this.description = description;
        this.modality = modality;
        this.species = species;
        this.imageUrl = imageUrl;
        this.lesionShapeType = lesionShapeType;
        this.lesionDataJson = lesionDataJson;
    }

    public void updateMetadata(
            String title,
            String description,
            Modality modality,
            Species species,
            String imageUrl,
            LesionShapeType lesionShapeType,
            String lesionDataJson
    ) {
        this.title = title;
        this.description = description;
        this.modality = modality;
        this.species = species;
        this.imageUrl = imageUrl;
        this.lesionShapeType = lesionShapeType;
        this.lesionDataJson = lesionDataJson;
    }

    public void incrementVersion() {
        this.version = this.version + 1;
    }

    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = Instant.now();
        }
    }

    public void restore() {
        this.deletedAt = null;
    }

    @PrePersist
    void onCreate() {
        if (this.version == null) {
            this.version = 1L;
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void replaceFindings(List<CaseFinding> newFindings) {
        this.findings.clear();
        if (newFindings == null) {
            return;
        }
        newFindings.forEach(this::addFinding);
    }

    public void replaceDiagnoses(List<CaseDiagnosis> newDiagnoses) {
        this.diagnoses.clear();
        if (newDiagnoses == null) {
            return;
        }
        newDiagnoses.forEach(this::addDiagnosis);
    }

    private void addFinding(CaseFinding caseFinding) {
        if (caseFinding == null) {
            return;
        }
        caseFinding.setImageCase(this);
        this.findings.add(caseFinding);
    }

    private void addDiagnosis(CaseDiagnosis caseDiagnosis) {
        if (caseDiagnosis == null) {
            return;
        }
        caseDiagnosis.setImageCase(this);
        this.diagnoses.add(caseDiagnosis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ImageCase imageCase = (ImageCase) o;
        return id != null && Objects.equals(id, imageCase.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
