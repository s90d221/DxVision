package com.example.dxvision.domain.dashboard.service;

import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.attempt.Attempt;
import com.example.dxvision.domain.dashboard.dto.DashboardActivityResponse;
import com.example.dxvision.domain.dashboard.dto.DashboardCaseItemResponse;
import com.example.dxvision.domain.dashboard.dto.DashboardSummaryResponse;
import com.example.dxvision.domain.progress.ProgressRules;
import com.example.dxvision.domain.progress.UserCaseProgress;
import com.example.dxvision.domain.progress.UserCaseStatus;
import com.example.dxvision.domain.repository.AttemptRepository;
import com.example.dxvision.domain.repository.UserCaseProgressRepository;
import com.example.dxvision.global.security.CurrentUserProvider;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final CurrentUserProvider currentUserProvider;
    private final UserCaseProgressRepository userCaseProgressRepository;
    private final AttemptRepository attemptRepository;

    private static final ZoneId ACTIVITY_ZONE_ID = ZoneId.of("Asia/Seoul");

    public DashboardService(
            CurrentUserProvider currentUserProvider,
            UserCaseProgressRepository userCaseProgressRepository,
            AttemptRepository attemptRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.userCaseProgressRepository = userCaseProgressRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        User user = currentUserProvider.getCurrentUser();
        long correctCount = userCaseProgressRepository.countByUserIdAndStatus(user.getId(), UserCaseStatus.CORRECT);
        long wrongCount = userCaseProgressRepository.countByUserIdAndStatus(user.getId(), UserCaseStatus.WRONG);
        long reattemptCount = userCaseProgressRepository.countByUserIdAndStatus(user.getId(), UserCaseStatus.REATTEMPT_CORRECT);

        int xp = calculateXp(correctCount, wrongCount, reattemptCount);
        int level = Math.max(1, xp / 100 + 1);
        int streak = (int) Math.min(correctCount + reattemptCount, 30);

        return new DashboardSummaryResponse(
                correctCount,
                wrongCount,
                reattemptCount,
                xp,
                level,
                streak,
                ProgressRules.CORRECT_THRESHOLD
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardCaseItemResponse> getCases(UserCaseStatus status) {
        User user = currentUserProvider.getCurrentUser();
        List<UserCaseProgress> progresses = userCaseProgressRepository.findByUserIdAndStatusWithCase(
                user.getId(),
                status
        );

        return progresses.stream()
                .map(progress -> new DashboardCaseItemResponse(
                        progress.getImageCase().getId(),
                        progress.getImageCase().getTitle(),
                        progress.getStatus(),
                        progress.getLastAttemptAt(),
                        progress.getLastScore()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardActivityResponse getActivity(int requestedDays) {
        int days = Math.min(Math.max(requestedDays, 1), 90);
        User user = currentUserProvider.getCurrentUser();

        LocalDate endDate = LocalDate.now(ACTIVITY_ZONE_ID);
        LocalDate startDate = endDate.minusDays(days - 1L);

        Instant startInstant = startDate.atStartOfDay(ACTIVITY_ZONE_ID).toInstant();
        Instant endInstantExclusive = endDate.plusDays(1L).atStartOfDay(ACTIVITY_ZONE_ID).toInstant();

        List<Attempt> attempts = attemptRepository.findByUserIdAndSubmittedAtBetween(
                user.getId(),
                startInstant,
                endInstantExclusive
        );

        Map<LocalDate, Long> attemptsByDay = attempts.stream()
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getSubmittedAt().atZone(ACTIVITY_ZONE_ID).toLocalDate(),
                        Collectors.counting()
                ));

        List<DashboardActivityResponse.DashboardActivityDay> daysPayload = new ArrayList<>();
        LocalDate cursor = startDate;
        long totalSolved = 0;
        while (!cursor.isAfter(endDate)) {
            long solvedCount = attemptsByDay.getOrDefault(cursor, 0L);
            totalSolved += solvedCount;
            daysPayload.add(new DashboardActivityResponse.DashboardActivityDay(cursor.toString(), solvedCount));
            cursor = cursor.plusDays(1);
        }

        int streak = calculateStreak(endDate, startDate, attemptsByDay);

        return new DashboardActivityResponse(daysPayload, totalSolved, streak);
    }

    private int calculateStreak(LocalDate endDate, LocalDate startDate, Map<LocalDate, Long> attemptsByDay) {
        int streak = 0;
        LocalDate cursor = endDate;
        while (!cursor.isBefore(startDate)) {
            long count = attemptsByDay.getOrDefault(cursor, 0L);
            if (count <= 0) {
                break;
            }
            streak += 1;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int calculateXp(long correct, long wrong, long reattemptCorrect) {
        long xp = correct * 50L + reattemptCorrect * 70L + wrong * 10L;
        return (int) Math.min(xp, Integer.MAX_VALUE);
    }
}
