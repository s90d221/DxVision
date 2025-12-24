package com.example.dxvision;

import com.example.dxvision.domain.auth.dto.LoginRequest;
import com.example.dxvision.domain.auth.dto.SignupRequest;
import com.example.dxvision.domain.attempt.dto.AttemptSubmitRequest;
import com.example.dxvision.domain.casefile.CaseDiagnosis;
import com.example.dxvision.domain.casefile.CaseFinding;
import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.casefile.ImageCase;
import com.example.dxvision.domain.casefile.LesionShapeType;
import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttemptSubmitApiTest {

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

    private ImageCase imageCase;

    @BeforeEach
    void setupCase() {
        if (imageCase != null) {
            return;
        }
        Finding f1 = findingRepository.save(new Finding("Finding One", "desc"));
        Diagnosis d1 = diagnosisRepository.save(new Diagnosis("Diagnosis One", "desc"));

        ImageCase ic = new ImageCase(
                "Test Case",
                "Desc",
                Modality.XRAY,
                Species.DOG,
                "http://example.com/img.jpg",
                LesionShapeType.CIRCLE,
                """
                {"type":"CIRCLE","cx":0.5,"cy":0.5,"r":0.2}
                """
        );
        CaseFinding cf = new CaseFinding(ic, f1, true);
        CaseDiagnosis cd = new CaseDiagnosis(ic, d1, 1.0);
        ic.getFindings().add(cf);
        ic.getDiagnoses().add(cd);
        imageCase = imageCaseRepository.save(ic);
    }

    private String signupAndLogin() throws Exception {
        String email = "attempt-user-" + UUID.randomUUID() + "@example.com";
        SignupRequest signupRequest = new SignupRequest(email, "Password123!", "Attempt User");

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

        return objectMapper.readTree(token).get("token").asText();
    }

    @Test
    void submitAttemptSuccess() throws Exception {
        String jwt = signupAndLogin();

        AttemptSubmitRequest req = new AttemptSubmitRequest(
                imageCase.getId(),
                imageCase.getVersion(),
                List.of(imageCase.getFindings().getFirst().getFinding().getId()),
                List.of(imageCase.getDiagnoses().getFirst().getDiagnosis().getId()),
                0.5,
                0.5
        );

        mockMvc.perform(post("/api/v1/attempts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").isNotEmpty())
                .andExpect(jsonPath("$.caseId").value(imageCase.getId()))
                .andExpect(jsonPath("$.caseVersion").value(imageCase.getVersion()))
                .andExpect(jsonPath("$.findingsScore").isNumber())
                .andExpect(jsonPath("$.locationScore").isNumber())
                .andExpect(jsonPath("$.diagnosisScore").isNumber())
                .andExpect(jsonPath("$.finalScore").isNumber());
    }

    @Test
    void submitAttemptVersionMismatch() throws Exception {
        String jwt = signupAndLogin();
        AttemptSubmitRequest req = new AttemptSubmitRequest(
                imageCase.getId(),
                imageCase.getVersion() + 1,
                List.of(),
                List.of(),
                0.1,
                0.1
        );

        mockMvc.perform(post("/api/v1/attempts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}
