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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "image_cases")
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

    @OneToMany(mappedBy = "imageCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CaseFinding> findings = new ArrayList<>();

    @OneToMany(mappedBy = "imageCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CaseDiagnosis> diagnoses = new ArrayList<>();

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
}
