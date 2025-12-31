package com.example.dxvision.domain.admin.service;

import com.example.dxvision.domain.admin.dto.AdminUserActivityDto;
import com.example.dxvision.domain.admin.dto.AdminUserCaseProgressDto;
import com.example.dxvision.domain.admin.dto.AdminUserDetailResponse;
import com.example.dxvision.domain.admin.dto.AdminUserListItem;
import com.example.dxvision.domain.admin.dto.AdminUserStats;
import com.example.dxvision.domain.admin.dto.AdminUserUpdateRequest;
import com.example.dxvision.domain.admin.dto.PageResponse;
import com.example.dxvision.domain.attempt.Attempt;
import com.example.dxvision.domain.auth.Role;
import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.auth.UserStatus;
import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.progress.ProgressRules;
import com.example.dxvision.domain.progress.UserCaseProgress;
import com.example.dxvision.domain.progress.UserCaseStatus;
import com.example.dxvision.domain.repository.AttemptRepository;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import com.example.dxvision.domain.repository.UserCaseProgressRepository;
import com.example.dxvision.domain.repository.UserProgressAggregate;
import com.example.dxvision.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {
    private static final AdminUserStats EMPTY_STATS = new AdminUserStats(0, 0, 0, 0, null);

    private final UserRepository userRepository;
    private final UserCaseProgressRepository userCaseProgressRepository;
    private final ImageCaseRepository imageCaseRepository;
    private final AttemptRepository attemptRepository;

    public AdminUserService(
            UserRepository userRepository,
            UserCaseProgressRepository userCaseProgressRepository,
            ImageCaseRepository imageCaseRepository,
            AttemptRepository attemptRepository
    ) {
        this.userRepository = userRepository;
        this.userCaseProgressRepository = userCaseProgressRepository;
        this.imageCaseRepository = imageCaseRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserListItem> listUsers(int page, int size, String q) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<User> users = userRepository.searchByEmailOrName(q, pageable);
        Map<Long, AdminUserStats> statsMap = buildStatsForUsers(users.getContent());

        List<AdminUserListItem> items = users.getContent().stream()
                .map(user -> new AdminUserListItem(
                        user.getId(),
                        user.getEmail(),
                        user.getName(),
                        user.getRole(),
                        user.getStatus(),
                        user.getCreatedAt(),
                        statsMap.getOrDefault(user.getId(), EMPTY_STATS)
                ))
                .toList();

        Page<AdminUserListItem> mapped = new PageImpl<>(items, pageable, users.getTotalElements());
        return PageResponse.of(mapped);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<UserCaseProgress> progresses = userCaseProgressRepository.findByUserIdWithCase(userId);
        Map<Long, UserCaseProgress> progressByCaseId = progresses.stream()
                .collect(Collectors.toMap(p -> p.getImageCase().getId(), Function.identity()));

        AdminUserStats stats = buildStatsFromProgress(progresses);
        List<AdminUserCaseProgressDto> caseProgress = buildCaseProgress(progressByCaseId);
        List<AdminUserActivityDto> activities = buildRecentActivities(userId, progressByCaseId);

        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                stats,
                caseProgress,
                activities
        );
    }

    @Transactional
    public AdminUserDetailResponse updateUserStatus(Long userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserStatus nextStatus = parseStatus(request.status());
        if (user.getRole() == Role.ADMIN && nextStatus == UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin accounts cannot be disabled");
        }

        if (user.getStatus() != nextStatus) {
            user.updateStatus(nextStatus);
            userRepository.save(user);
        }

        return getUserDetail(userId);
    }

    private Map<Long, AdminUserStats> buildStatsForUsers(List<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, UserProgressAggregate> aggregates = userCaseProgressRepository.aggregateByUserIds(userIds)
                .stream()
                .collect(Collectors.toMap(UserProgressAggregate::getUserId, Function.identity(), (a, b) -> a, HashMap::new));

        Map<Long, AdminUserStats> statsMap = new HashMap<>();
        for (User user : users) {
            UserProgressAggregate aggregate = aggregates.get(user.getId());
            statsMap.put(user.getId(), aggregate == null ? EMPTY_STATS : buildStatsFromAggregate(aggregate));
        }
        return statsMap;
    }

    private AdminUserStats buildStatsFromAggregate(UserProgressAggregate aggregate) {
        long correctAttempts = nullSafeLong(aggregate.getCorrectAttempts());
        long wrongAttempts = nullSafeLong(aggregate.getWrongAttempts());
        long attemptedCount = correctAttempts + wrongAttempts;
        long correctCount = nullSafeLong(aggregate.getCorrectCases());
        long wrongCount = nullSafeLong(aggregate.getWrongCases());
        long reattemptCorrectCount = nullSafeLong(aggregate.getReattemptCorrectCases());
        return new AdminUserStats(
                attemptedCount,
                correctCount,
                wrongCount,
                reattemptCorrectCount,
                aggregate.getLastAttemptAt()
        );
    }

    private AdminUserStats buildStatsFromProgress(List<UserCaseProgress> progresses) {
        long attemptedCount = progresses.stream()
                .mapToLong(p -> (long) p.getCorrectCount() + p.getWrongCount())
                .sum();
        long correctCount = progresses.stream().filter(p -> p.getStatus() == UserCaseStatus.CORRECT).count();
        long wrongCount = progresses.stream().filter(p -> p.getStatus() == UserCaseStatus.WRONG).count();
        long reattemptCorrectCount = progresses.stream()
                .filter(p -> p.getStatus() == UserCaseStatus.REATTEMPT_CORRECT)
                .count();
        Instant lastActiveAt = progresses.stream()
                .map(UserCaseProgress::getLastAttemptAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new AdminUserStats(attemptedCount, correctCount, wrongCount, reattemptCorrectCount, lastActiveAt);
    }

    private List<AdminUserCaseProgressDto> buildCaseProgress(Map<Long, UserCaseProgress> progressByCaseId) {
        return imageCaseRepository.findAll().stream()
                .map(imageCase -> {
                    UserCaseProgress progress = progressByCaseId.get(imageCase.getId());
                    if (progress == null) {
                        return new AdminUserCaseProgressDto(
                                imageCase.getId(),
                                imageCase.getTitle(),
                                UserCaseStatus.UNSEEN,
                                0,
                                null
                        );
                    }
                    int attemptCount = progress.getCorrectCount() + progress.getWrongCount();
                    return new AdminUserCaseProgressDto(
                            imageCase.getId(),
                            imageCase.getTitle(),
                            progress.getStatus(),
                            attemptCount,
                            progress.getLastAttemptAt()
                    );
                })
                .sorted(Comparator.comparing(AdminUserCaseProgressDto::caseTitle))
                .toList();
    }

    private List<AdminUserActivityDto> buildRecentActivities(Long userId, Map<Long, UserCaseProgress> progressByCaseId) {
        List<Attempt> attempts = attemptRepository.findRecentAttempts(userId, PageRequest.of(0, 20));
        return attempts.stream()
                .map(attempt -> {
                    UserCaseProgress progress = progressByCaseId.get(attempt.getImageCase().getId());
                    UserCaseStatus status = resolveAttemptStatus(attempt, progress);
                    return new AdminUserActivityDto(
                            attempt.getSubmittedAt(),
                            attempt.getImageCase().getId(),
                            attempt.getImageCase().getTitle(),
                            status,
                            attempt.getFinalScore()
                    );
                })
                .toList();
    }

    private UserCaseStatus resolveAttemptStatus(Attempt attempt, UserCaseProgress progress) {
        boolean isCorrect = attempt.getFinalScore() >= ProgressRules.CORRECT_THRESHOLD;
        if (!isCorrect) {
            return UserCaseStatus.WRONG;
        }
        if (progress != null && progress.getStatus() == UserCaseStatus.REATTEMPT_CORRECT) {
            return UserCaseStatus.REATTEMPT_CORRECT;
        }
        return UserCaseStatus.CORRECT;
    }

    private UserStatus parseStatus(String value) {
        try {
            return UserStatus.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
    }

    private long nullSafeLong(Number number) {
        return number == null ? 0 : number.longValue();
    }
}
