package com.example.dxvision.domain.attempt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attempt_location_answers")
@Getter
@NoArgsConstructor
public class AttemptLocationAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false, unique = true)
    private Attempt attempt;

    @Column(nullable = false)
    private Double clickX;

    @Column(nullable = false)
    private Double clickY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocationGrade locationGrade;

    public AttemptLocationAnswer(Attempt attempt, Double clickX, Double clickY, LocationGrade locationGrade) {
        this.attempt = attempt;
        this.clickX = clickX;
        this.clickY = clickY;
        this.locationGrade = locationGrade;
    }
}
