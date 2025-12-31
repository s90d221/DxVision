package com.example.dxvision.domain.casefile;

import jakarta.persistence.CascadeType;
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
        name = "finding_folders",
        uniqueConstraints = @UniqueConstraint(name = "uk_folder_finding", columnNames = {"folder_id", "finding_id"})
)
@Getter
@NoArgsConstructor
public class FindingFolder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "folder_id")
    private OptionFolder folder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id")
    private Finding finding;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    public FindingFolder(OptionFolder folder, Finding finding, Integer sortOrder) {
        this.folder = folder;
        this.finding = finding;
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
        FindingFolder that = (FindingFolder) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
