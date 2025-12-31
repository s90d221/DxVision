package com.example.dxvision.domain.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.Hibernate;

@Entity
@Table(name = "option_folders")
@Getter
@NoArgsConstructor
public class OptionFolder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OptionType type;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private boolean systemDefault = false;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "folder", orphanRemoval = true)
    private List<FindingFolder> findingMappings = new ArrayList<>();

    @OneToMany(mappedBy = "folder", orphanRemoval = true)
    private List<DiagnosisFolder> diagnosisMappings = new ArrayList<>();

    public OptionFolder(OptionType type, String name, Integer sortOrder, boolean systemDefault) {
        this.type = type;
        this.name = name;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.systemDefault = systemDefault;
    }

    public void update(String name, Integer sortOrder) {
        this.name = name;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    public void markAsDefault() {
        this.systemDefault = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        OptionFolder that = (OptionFolder) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
