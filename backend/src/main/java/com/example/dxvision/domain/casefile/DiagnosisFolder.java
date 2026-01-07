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
import org.hibernate.Hibernate;

@Entity
@Table(
        name = "diagnosis_folders",
        uniqueConstraints = @UniqueConstraint(name = "uk_folder_diagnosis", columnNames = {"folder_id", "diagnosis_id"})
)
@Getter
@NoArgsConstructor
public class DiagnosisFolder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ManyToOne(자식 -> 부모) 관계에서는 일반적으로 cascade를 두지 않습니다.
     * (자식 저장 시 부모까지 persist/merge하려다가 detached entity 예외가 자주 발생)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private OptionFolder folder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private Diagnosis diagnosis;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    public DiagnosisFolder(OptionFolder folder, Diagnosis diagnosis, Integer sortOrder) {
        this.folder = folder;
        this.diagnosis = diagnosis;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public void updateSortOrder(Integer sortOrder) {
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        DiagnosisFolder that = (DiagnosisFolder) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
