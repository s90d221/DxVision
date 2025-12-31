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
import com.example.dxvision.domain.casefile.dto.CaseListItemResponse;
import com.example.dxvision.domain.casefile.dto.CaseListPageResponse;
import com.example.dxvision.domain.progress.UserCaseStatus;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CaseListApiTest {

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

    private ImageCase thoraxDog;
    private ImageCase abdomenCat;
    private ImageCase cardioDog;

    @BeforeEach
    void setUpCases() {
        if (thoraxDog == null) {
            thoraxDog = createCase("Thorax Mass", "Thorax mass canine", Modality.XRAY, Species.DOG);
        }
        if (abdomenCat == null) {
            abdomenCat = createCase("Abdomen Lesion", "Abdomen lesion feline", Modality.ULTRASOUND, Species.CAT);
        }
        if (cardioDog == null) {
            cardioDog = createCase("Cardio Check", "Cardio check canine", Modality.CT, Species.DOG);
        }
    }

    @Test
    void unseenStatusFiltersOutAttemptedCases() throws Exception {
        String jwt = signupAndLogin("unseen");

        submitAttempt(jwt, correctAttempt(thoraxDog));

        String json = mockMvc.perform(get("/api/v1/cases")
                        .param("status", "UNSEEN")
                        .param("sort", "title,asc")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CaseListPageResponse<CaseListItemResponse> page = objectMapper.readValue(
                json, new TypeReference<>() {});

        assertThat(page.content())
                .extracting(CaseListItemResponse::caseId)
                .containsExactlyInAnyOrder(abdomenCat.getId(), cardioDog.getId());
        assertThat(page.content())
                .allSatisfy(item -> {
                    assertThat(item.status()).isEqualTo(UserCaseStatus.UNSEEN);
                    assertThat(item.lastAttemptAt()).isNull();
                    assertThat(item.lastScore()).isNull();
                });
    }

    @Test
    void keywordAndSpeciesFiltersReduceResultSet() throws Exception {
        String jwt = signupAndLogin("filters");

        String keywordJson = mockMvc.perform(get("/api/v1/cases")
                        .param("keyword", "thorax")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CaseListPageResponse<CaseListItemResponse> keywordPage = objectMapper.readValue(
                keywordJson, new TypeReference<>() {});
        assertThat(keywordPage.content()).hasSize(1);
        assertThat(keywordPage.content().getFirst().caseId()).isEqualTo(thoraxDog.getId());

        String modalitySpeciesJson = mockMvc.perform(get("/api/v1/cases")
                        .param("modality", "ULTRASOUND")
                        .param("species", "CAT")
                        .param("sort", "title,desc")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CaseListPageResponse<CaseListItemResponse> modalityPage = objectMapper.readValue(
                modalitySpeciesJson, new TypeReference<>() {});
        assertThat(modalityPage.content()).hasSize(1);
        CaseListItemResponse only = modalityPage.content().getFirst();
        assertThat(only.caseId()).isEqualTo(abdomenCat.getId());
        assertThat(only.species()).isEqualTo(Species.CAT);
        assertThat(only.modality()).isEqualTo(Modality.ULTRASOUND);
    }

    @Test
    void statusFilterReturnsProgressDetails() throws Exception {
        String jwt = signupAndLogin("progress");

        submitAttempt(jwt, correctAttempt(thoraxDog));
        submitAttempt(jwt, wrongAttempt(abdomenCat));

        CaseListPageResponse<CaseListItemResponse> correctPage = fetchCases(jwt, "CORRECT");
        assertThat(correctPage.content()).hasSize(1);
        CaseListItemResponse correct = correctPage.content().getFirst();
        assertThat(correct.caseId()).isEqualTo(thoraxDog.getId());
        assertThat(correct.status()).isEqualTo(UserCaseStatus.CORRECT);
        assertThat(correct.lastScore()).isNotNull();
        assertThat(correct.lastAttemptAt()).isNotNull();

        CaseListPageResponse<CaseListItemResponse> wrongPage = fetchCases(jwt, "WRONG");
        assertThat(wrongPage.content()).hasSize(1);
        CaseListItemResponse wrong = wrongPage.content().getFirst();
        assertThat(wrong.caseId()).isEqualTo(abdomenCat.getId());
        assertThat(wrong.status()).isEqualTo(UserCaseStatus.WRONG);
        assertThat(wrong.lastScore()).isNotNull();
        assertThat(wrong.lastAttemptAt()).isNotNull();
    }

    private CaseListPageResponse<CaseListItemResponse> fetchCases(String jwt, String status) throws Exception {
        String json = mockMvc.perform(get("/api/v1/cases")
                        .param("status", status)
                        .param("size", "5")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private String signupAndLogin(String label) throws Exception {
        String email = "cases-" + label + "-" + UUID.randomUUID() + "@example.com";
        SignupRequest signupRequest = new SignupRequest(email, "Password123!", "Case " + label);

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

    private void submitAttempt(String jwt, AttemptSubmitRequest request) throws Exception {
        mockMvc.perform(post("/api/v1/attempts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
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

    private ImageCase createCase(String title, String description, Modality modality, Species species) {
        Finding finding = findingRepository.save(new Finding(title + " Finding", "desc"));
        Diagnosis diagnosis = diagnosisRepository.save(new Diagnosis(title + " Diagnosis", "desc"));

        ImageCase imageCase = new ImageCase(
                title,
                description,
                modality,
                species,
                "https://placehold.co/800x600?text=" + title.replace(" ", "+"),
                LesionShapeType.CIRCLE,
                """
                        {"type":"CIRCLE","cx":0.5,"cy":0.5,"r":0.2}
                        """
        );

        imageCase.getFindings().add(new CaseFinding(imageCase, finding, true));
        imageCase.getDiagnoses().add(new CaseDiagnosis(imageCase, diagnosis, 1.0));
        return imageCaseRepository.save(imageCase);
    }
}
