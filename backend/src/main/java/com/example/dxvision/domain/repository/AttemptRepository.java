package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.attempt.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
}
