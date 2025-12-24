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

    @Column(nullable = false, length = 200)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Finding(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
