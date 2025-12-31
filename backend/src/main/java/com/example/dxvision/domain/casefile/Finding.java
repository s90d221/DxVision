package com.example.dxvision.domain.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "findings")
@Getter
@NoArgsConstructor
public class Finding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 중복 방지(가능하면 DB 레벨에서도 보장)
    @Column(nullable = false, length = 200, unique = true)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Finding(String label, String description) {
        this.label = normalizeLabel(label);
        this.description = description;
    }

    public void update(String label, String description) {
        this.label = normalizeLabel(label);
        this.description = description;
    }

    private String normalizeLabel(String label) {
        if (label == null) {
            return null;
        }
        return label.trim();
    }
}
