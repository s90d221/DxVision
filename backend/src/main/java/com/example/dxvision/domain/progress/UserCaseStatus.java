package com.example.dxvision.domain.progress;

import java.util.Optional;

public enum UserCaseStatus {
    CORRECT,
    WRONG,
    REATTEMPT_CORRECT,
    /**
     * Legacy alias kept for compatibility with existing rows.
     * Use {@link #UNSEEN} for new responses/filters.
     */
    UNATTEMPTED,
    UNSEEN;

    public static Optional<UserCaseStatus> fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase();
        if ("UNSEEN".equals(normalized) || "UNATTEMPTED".equals(normalized)) {
            return Optional.of(UNSEEN);
        }
        try {
            return Optional.of(UserCaseStatus.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static UserCaseStatus normalize(UserCaseStatus status) {
        if (status == null) {
            return UNSEEN;
        }
        return status == UNATTEMPTED ? UNSEEN : status;
    }

    public boolean isUnseen() {
        return this == UNSEEN || this == UNATTEMPTED;
    }
}
