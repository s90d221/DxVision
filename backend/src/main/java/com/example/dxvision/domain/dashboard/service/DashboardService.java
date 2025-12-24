package com.example.dxvision.domain.dashboard.service;

import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.dashboard.dto.DashboardCaseItemResponse;
import com.example.dxvision.domain.dashboard.dto.DashboardSummaryResponse;
import com.example.dxvision.domain.progress.ProgressRules;
import com.example.dxvision.domain.progress.UserCaseProgress;
import com.example.dxvision.domain.progress.UserCaseStatus;
import com.example.dxvision.domain.repository.UserCaseProgressRepository;
import com.example.dxvision.global.security.CurrentUserProvider;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final CurrentUserProvider currentUserProvider;
    private final UserCaseProgressRepository userCaseProgressRepository;

    public DashboardService(
            CurrentUserProvider currentUserProvider,
            UserCaseProgressRepository userCaseProgressRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.userCaseProgressRepository = userCaseProgressRepository;
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

    private int calculateXp(long correct, long wrong, long reattemptCorrect) {
        long xp = correct * 50L + reattemptCorrect * 70L + wrong * 10L;
        return (int) Math.min(xp, Integer.MAX_VALUE);
    }
}
