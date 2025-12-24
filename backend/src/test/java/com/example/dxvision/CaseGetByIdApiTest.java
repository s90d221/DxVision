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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CaseGetByIdApiTest {
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
        Finding f1 = findingRepository.save(new Finding("Finding A", "desc"));
        Diagnosis d1 = diagnosisRepository.save(new Diagnosis("Diagnosis X", "desc"));

        ImageCase ic = new ImageCase(
                "Case By Id",
                "Desc",
                Modality.XRAY,
                Species.DOG,
                "http://example.com/img.jpg",
                LesionShapeType.CIRCLE,
                """
                {"type":"CIRCLE","cx":0.5,"cy":0.5,"r":0.2}
                """
        );
        ic.getFindings().add(new CaseFinding(ic, f1, true));
        ic.getDiagnoses().add(new CaseDiagnosis(ic, d1, 1.0));
        imageCase = imageCaseRepository.save(ic);
    }

    private String signupAndLogin() throws Exception {
        String email = "case-by-id-" + UUID.randomUUID() + "@example.com";
        SignupRequest signupRequest = new SignupRequest(email, "Password123!", "Case User");

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
    void getCaseByIdReturnsSafeDto() throws Exception {
        String jwt = signupAndLogin();

        mockMvc.perform(get("/api/v1/cases/{id}", imageCase.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageCase.getId()))
                .andExpect(jsonPath("$.title").value("Case By Id"))
                .andExpect(jsonPath("$.imageUrl").value("http://example.com/img.jpg"))
                .andExpect(jsonPath("$.findings").isArray())
                .andExpect(jsonPath("$.diagnoses").isArray());
    }
}
