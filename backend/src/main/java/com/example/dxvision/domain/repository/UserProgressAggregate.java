package com.example.dxvision.domain.repository;

import java.time.Instant;

public interface UserProgressAggregate {
    Long getUserId();

    Long getCorrectAttempts();

    Long getWrongAttempts();

    Long getCorrectCases();

    Long getWrongCases();

    Long getReattemptCorrectCases();

    Instant getLastAttemptAt();
}
