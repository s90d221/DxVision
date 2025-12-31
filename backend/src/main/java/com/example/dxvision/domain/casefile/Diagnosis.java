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
@Table(name = "diagnoses")
@Getter
@NoArgsConstructor
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 중복 방지(가능하면 DB 레벨에서도 보장)
    @Column(nullable = false, length = 200, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Diagnosis(String name, String description) {
        this.name = normalizeName(name);
        this.description = description;
    }

    public void update(String name, String description) {
        this.name = normalizeName(name);
        this.description = description;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim();
    }
}
