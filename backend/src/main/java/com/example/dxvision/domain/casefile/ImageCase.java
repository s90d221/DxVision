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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    /**
     * Set으로 유지 (MultipleBagFetchException 회피)
     */
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

    /**
     * IMPORTANT:
     * - clear() 후 재삽입은 flush 순서(INSERT -> DELETE) 때문에
     *   (image_case_id, finding_id) 유니크가 있으면 Duplicate가 날 수 있음.
     * - 그래서 "diff 방식"으로 기존 엔티티를 재사용/업데이트한다.
     */
    public void replaceFindings(List<CaseFinding> newFindings) {
        List<CaseFinding> incoming = (newFindings == null) ? List.of() : newFindings;

        // 현재 상태를 findingId 기준으로 인덱싱
        Map<Long, CaseFinding> currentByFindingId = new HashMap<>();
        for (CaseFinding cf : this.findings) {
            if (cf.getFinding() != null && cf.getFinding().getId() != null) {
                currentByFindingId.put(cf.getFinding().getId(), cf);
            }
        }

        // 들어온 목록의 findingId set
        Set<Long> desiredFindingIds = new HashSet<>();
        for (CaseFinding cf : incoming) {
            if (cf == null || cf.getFinding() == null || cf.getFinding().getId() == null) {
                continue;
            }
            desiredFindingIds.add(cf.getFinding().getId());
        }

        // 1) 제거: 기존 중에 더 이상 필요 없는 것 제거 (orphanRemoval -> delete)
        Iterator<CaseFinding> it = this.findings.iterator();
        while (it.hasNext()) {
            CaseFinding existing = it.next();
            Long fid = (existing.getFinding() != null) ? existing.getFinding().getId() : null;
            if (fid != null && !desiredFindingIds.contains(fid)) {
                it.remove();
                existing.setImageCase(null); // 명시적으로 끊어줌(안전)
            }
        }

        // 2) 업데이트/추가
        for (CaseFinding req : incoming) {
            if (req == null || req.getFinding() == null || req.getFinding().getId() == null) {
                continue;
            }

            Long fid = req.getFinding().getId();
            CaseFinding existing = currentByFindingId.get(fid);

            if (existing != null && this.findings.contains(existing)) {
                // 기존 엔티티 재사용: required만 업데이트
                existing.updateRequiredFinding(req.isRequiredFinding());
            } else {
                // 신규: 새 엔티티 생성해서 추가
                addFinding(new CaseFinding(req.getFinding(), req.isRequiredFinding()));
            }
        }
    }

    public void replaceDiagnoses(List<CaseDiagnosis> newDiagnoses) {
        List<CaseDiagnosis> incoming = (newDiagnoses == null) ? List.of() : newDiagnoses;

        Map<Long, CaseDiagnosis> currentByDiagnosisId = new HashMap<>();
        for (CaseDiagnosis cd : this.diagnoses) {
            if (cd.getDiagnosis() != null && cd.getDiagnosis().getId() != null) {
                currentByDiagnosisId.put(cd.getDiagnosis().getId(), cd);
            }
        }

        Set<Long> desiredDiagnosisIds = new HashSet<>();
        for (CaseDiagnosis cd : incoming) {
            if (cd == null || cd.getDiagnosis() == null || cd.getDiagnosis().getId() == null) {
                continue;
            }
            desiredDiagnosisIds.add(cd.getDiagnosis().getId());
        }

        Iterator<CaseDiagnosis> it = this.diagnoses.iterator();
        while (it.hasNext()) {
            CaseDiagnosis existing = it.next();
            Long did = (existing.getDiagnosis() != null) ? existing.getDiagnosis().getId() : null;
            if (did != null && !desiredDiagnosisIds.contains(did)) {
                it.remove();
                existing.setImageCase(null);
            }
        }

        for (CaseDiagnosis req : incoming) {
            if (req == null || req.getDiagnosis() == null || req.getDiagnosis().getId() == null) {
                continue;
            }

            Long did = req.getDiagnosis().getId();
            CaseDiagnosis existing = currentByDiagnosisId.get(did);

            if (existing != null && this.diagnoses.contains(existing)) {
                existing.updateWeight(req.getWeight());
            } else {
                addDiagnosis(new CaseDiagnosis(req.getDiagnosis(), req.getWeight()));
            }
        }
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
