package com.example.dxvision;

import com.example.dxvision.domain.auth.dto.LoginRequest;
import com.example.dxvision.domain.auth.dto.SignupRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CaseRandomApiTest {

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

    @BeforeEach
    void setupData() {
        if (imageCaseRepository.count() > 0) {
            return;
        }

        Finding f1 = findingRepository.save(new Finding("Finding A", "desc"));
        Finding f2 = findingRepository.save(new Finding("Finding B", "desc"));
        Diagnosis d1 = diagnosisRepository.save(new Diagnosis("Diagnosis X", "desc"));
        Diagnosis d2 = diagnosisRepository.save(new Diagnosis("Diagnosis Y", "desc"));

        ImageCase case1 = new ImageCase(
                "Case 1",
                "Desc 1",
                Modality.XRAY,
                Species.DOG,
                "http://example.com/image1.jpg",
                LesionShapeType.CIRCLE,
                """
                {"type":"CIRCLE","cx":0.5,"cy":0.5,"r":0.2}
                """
        );
        CaseFinding cf1 = new CaseFinding(case1, f1, true);
        CaseDiagnosis cd1 = new CaseDiagnosis(case1, d1, 1.0);
        case1.getFindings().add(cf1);
        case1.getDiagnoses().add(cd1);

        ImageCase case2 = new ImageCase(
                "Case 2",
                "Desc 2",
                Modality.ULTRASOUND,
                Species.CAT,
                "http://example.com/image2.jpg",
                LesionShapeType.CIRCLE,
                """
                {"type":"CIRCLE","cx":0.4,"cy":0.6,"r":0.15}
                """
        );
        CaseFinding cf2 = new CaseFinding(case2, f2, true);
        CaseDiagnosis cd2 = new CaseDiagnosis(case2, d2, 1.0);
        case2.getFindings().add(cf2);
        case2.getDiagnoses().add(cd2);

        imageCaseRepository.saveAll(List.of(case1, case2));
    }

    @Test
    void randomCaseEndpointReturnsDataForAuthenticatedUser() throws Exception {
        String email = "case-user-" + UUID.randomUUID() + "@example.com";
        SignupRequest signupRequest = new SignupRequest(email, "Password123!", "Case User");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));

        LoginRequest loginRequest = new LoginRequest(email, "Password123!");
        String token = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jwt = objectMapper.readTree(token).get("token").asText();

        mockMvc.perform(get("/api/v1/cases/random")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.lesionShapeType").isNotEmpty())
                .andExpect(jsonPath("$.findings").isArray())
                .andExpect(jsonPath("$.diagnoses").isArray());
    }
}
