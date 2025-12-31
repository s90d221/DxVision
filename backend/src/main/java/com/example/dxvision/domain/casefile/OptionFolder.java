package com.example.dxvision.domain.casefile;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    private OptionFolderType type;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Integer orderIndex = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public OptionFolder(OptionFolderType type, String name, Integer orderIndex) {
        this.type = type;
        this.name = normalizeName(name);
        this.orderIndex = orderIndex != null ? orderIndex : 0;
    }

    public void update(String name, Integer orderIndex) {
        if (name != null && !name.isBlank()) {
            this.name = normalizeName(name);
        }
        if (orderIndex != null) {
            this.orderIndex = orderIndex;
        }
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim();
    }
}
