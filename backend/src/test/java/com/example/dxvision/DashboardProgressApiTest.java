package com.example.dxvision;

import com.example.dxvision.domain.auth.dto.LoginRequest;
import com.example.dxvision.domain.auth.dto.SignupRequest;
import com.example.dxvision.domain.attempt.Attempt;
import com.example.dxvision.domain.attempt.dto.AttemptSubmitRequest;
import com.example.dxvision.domain.casefile.CaseDiagnosis;
import com.example.dxvision.domain.casefile.CaseFinding;
import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.casefile.LesionShapeType;
import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import com.example.dxvision.domain.dashboard.dto.DashboardActivityResponse;
import com.example.dxvision.domain.dashboard.dto.DashboardCaseItemResponse;
import com.example.dxvision.domain.dashboard.dto.DashboardSummaryResponse;
import com.example.dxvision.domain.repository.AttemptRepository;
import com.example.dxvision.domain.progress.UserCaseStatus;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardProgressApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImageCaseRepository imageCaseRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    private ImageCase imageCaseOne;
    private ImageCase imageCaseTwo;

    @Autowired
    private AttemptRepository attemptRepository;

    @BeforeEach
    void setupCases() {
        if (imageCaseOne == null) {
            imageCaseOne = createCase("One");
        }
        if (imageCaseTwo == null) {
            imageCaseTwo = createCase("Two");
        }
    }

    @Test
    void statusTransitionsFollowRules() throws Exception {
        AuthContext auth = signupAndLogin("transition");

        submitAttempt(auth.jwt(), wrongAttempt(imageCaseOne));
        DashboardSummaryResponse afterWrong = fetchSummary(auth.jwt());
        assertThat(afterWrong.wrongCount()).isEqualTo(1);
        assertThat(afterWrong.correctCount()).isZero();

        submitAttempt(auth.jwt(), correctAttempt(imageCaseOne));
        DashboardSummaryResponse afterReattempt = fetchSummary(auth.jwt());
        assertThat(afterReattempt.reattemptCorrectCount()).isEqualTo(1);
        assertThat(afterReattempt.wrongCount()).isZero();

        List<DashboardCaseItemResponse> reattemptCases = fetchCases(auth.jwt(), UserCaseStatus.REATTEMPT_CORRECT);
        assertThat(reattemptCases).hasSize(1);
        DashboardCaseItemResponse item = reattemptCases.getFirst();
        assertThat(item.caseId()).isEqualTo(imageCaseOne.getId());
        assertThat(item.lastAttemptAt()).isNotNull();
        assertThat(item.lastScore()).isNotNull();

        submitAttempt(auth.jwt(), wrongAttempt(imageCaseOne));
        DashboardSummaryResponse afterBackToWrong = fetchSummary(auth.jwt());
        assertThat(afterBackToWrong.wrongCount()).isEqualTo(1);
        assertThat(afterBackToWrong.reattemptCorrectCount()).isZero();
    }

    @Test
    void summaryAndCasesReflectMixedStatuses() throws Exception {
        AuthContext auth = signupAndLogin("mixed");

        submitAttempt(auth.jwt(), correctAttempt(imageCaseOne));
        submitAttempt(auth.jwt(), wrongAttempt(imageCaseTwo));

        DashboardSummaryResponse summary = fetchSummary(auth.jwt());
        assertThat(summary.correctCount()).isEqualTo(1);
        assertThat(summary.wrongCount()).isEqualTo(1);
        assertThat(summary.reattemptCorrectCount()).isZero();
        assertThat(summary.level()).isGreaterThanOrEqualTo(1);
        assertThat(summary.correctThreshold()).isGreaterThan(0);

        List<DashboardCaseItemResponse> correctCases = fetchCases(auth.jwt(), UserCaseStatus.CORRECT);
        assertThat(correctCases).extracting(DashboardCaseItemResponse::caseId).containsExactly(imageCaseOne.getId());

        List<DashboardCaseItemResponse> wrongCases = fetchCases(auth.jwt(), UserCaseStatus.WRONG);
        assertThat(wrongCases).extracting(DashboardCaseItemResponse::caseId).containsExactly(imageCaseTwo.getId());
    }

    @Test
    void activityEndpointBucketsAttemptsAndReportsStreak() throws Exception {
        AuthContext auth = signupAndLogin("activity");

        submitAttempt(auth.jwt(), correctAttempt(imageCaseOne));
        submitAttempt(auth.jwt(), wrongAttempt(imageCaseTwo));
        submitAttempt(auth.jwt(), wrongAttempt(imageCaseOne));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate yesterday = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);

        List<Attempt> attempts = attemptRepository.findAll().stream()
                .filter(attempt -> attempt.getUser().getEmail().equals(auth.email()))
                .limit(3)
                .toList();

        assertThat(attempts).hasSize(3);

        if (attempts.size() == 3) {
            Instant todayInstant = today.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
            Instant yesterdayInstant = yesterday.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
            Instant twoDaysAgoInstant = twoDaysAgo.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();

            ReflectionTestUtils.setField(attempts.get(0), "submittedAt", todayInstant);
            ReflectionTestUtils.setField(attempts.get(1), "submittedAt", yesterdayInstant);
            ReflectionTestUtils.setField(attempts.get(2), "submittedAt", twoDaysAgoInstant);
            attemptRepository.saveAll(attempts);
        }

        DashboardActivityResponse activity = fetchActivity(auth.jwt(), 7);
        assertThat(activity.days()).hasSize(7);
        assertThat(activity.totalSolved()).isEqualTo(3);
        assertThat(activity.streak()).isGreaterThanOrEqualTo(3);

        long todayCount = activity.days().stream()
                .filter(day -> day.date().equals(today.toString()))
                .mapToLong(DashboardActivityResponse.DashboardActivityDay::solvedCount)
                .sum();
        assertThat(todayCount).isEqualTo(1);
    }

    private AuthContext signupAndLogin(String label) throws Exception {
        String email = "dashboard-" + label + "-" + UUID.randomUUID() + "@example.com";
        SignupRequest signupRequest = new SignupRequest(email, "Password123!", "Dash " + label);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, "Password123!");
        String token = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jwt = objectMapper.readTree(token).get("token").asText();
        return new AuthContext(jwt, email);
    }

    private void submitAttempt(String jwt, AttemptSubmitRequest request) throws Exception {
        mockMvc.perform(post("/api/v1/attempts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private DashboardSummaryResponse fetchSummary(String jwt) throws Exception {
        String json = mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(json, DashboardSummaryResponse.class);
    }

    private List<DashboardCaseItemResponse> fetchCases(String jwt, UserCaseStatus status) throws Exception {
        String json = mockMvc.perform(get("/api/v1/dashboard/cases")
                        .param("status", status.name())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private DashboardActivityResponse fetchActivity(String jwt, int days) throws Exception {
        String json = mockMvc.perform(get("/api/v1/dashboard/activity")
                        .param("days", String.valueOf(days))
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(json, DashboardActivityResponse.class);
    }

    private AttemptSubmitRequest correctAttempt(ImageCase imageCase) {
        Long findingId = imageCase.getFindings().iterator().next().getFinding().getId();
        Long diagnosisId = imageCase.getDiagnoses().iterator().next().getDiagnosis().getId();
        return new AttemptSubmitRequest(
                imageCase.getId(),
                imageCase.getVersion(),
                List.of(findingId),
                List.of(diagnosisId),
                0.5,
                0.5
        );
    }

    private AttemptSubmitRequest wrongAttempt(ImageCase imageCase) {
        return new AttemptSubmitRequest(
                imageCase.getId(),
                imageCase.getVersion(),
                List.of(),
                List.of(),
                0.0,
                0.0
        );
    }

    private ImageCase createCase(String label) {
        Finding finding = findingRepository.save(new Finding("Finding " + label, "desc"));
        Diagnosis diagnosis = diagnosisRepository.save(new Diagnosis("Diagnosis " + label, "desc"));

        ImageCase imageCase = new ImageCase(
                "Case " + label,
                "Description " + label,
                Modality.XRAY,
                Species.DOG,
                "https://placehold.co/800x600?text=Case+" + label,
                LesionShapeType.CIRCLE,
                """
                {"type":"CIRCLE","cx":0.5,"cy":0.5,"r":0.2}
                """
        );

        imageCase.getFindings().add(new CaseFinding(imageCase, finding, true));
        imageCase.getDiagnoses().add(new CaseDiagnosis(imageCase, diagnosis, 1.0));
        return imageCaseRepository.save(imageCase);
    }

    private record AuthContext(String jwt, String email) {
    }
}
