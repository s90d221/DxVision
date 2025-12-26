package com.example.dxvision;

import com.example.dxvision.domain.auth.Role;
import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.auth.dto.LoginRequest;
import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import com.example.dxvision.domain.repository.CaseDiagnosisRepository;
import com.example.dxvision.domain.repository.CaseFindingRepository;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import com.example.dxvision.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private CaseFindingRepository caseFindingRepository;

    @Autowired
    private CaseDiagnosisRepository caseDiagnosisRepository;

    @Autowired
    private ImageCaseRepository imageCaseRepository;

    @BeforeEach
    void cleanDatabase() {
        caseDiagnosisRepository.deleteAll();
        caseFindingRepository.deleteAll();
        imageCaseRepository.deleteAll();
        diagnosisRepository.deleteAll();
        findingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void nonAdminIsForbiddenFromAdminEndpoints() throws Exception {
        String token = createUserAndLogin(Role.USER);

        mockMvc.perform(get("/api/v1/admin/lookups").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseLookupsAndManageCasesWithMultipart() throws Exception {
        String token = createUserAndLogin(Role.ADMIN);
        Finding finding1 = findingRepository.save(new Finding("Opacity", ""));
        Finding finding2 = findingRepository.save(new Finding("Nodule", ""));
        Diagnosis diagnosis1 = diagnosisRepository.save(new Diagnosis("Pneumonia", ""));
        Diagnosis diagnosis2 = diagnosisRepository.save(new Diagnosis("Fracture", ""));

        // lookups
        mockMvc.perform(get("/api/v1/admin/lookups").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings.length()").value(2))
                .andExpect(jsonPath("$.diagnoses.length()").value(2));

        String findingsJson = objectMapper.writeValueAsString(List.of(
                Map.of("findingId", finding1.getId(), "required", true),
                Map.of("findingId", finding2.getId(), "required", false)
        ));
        String diagnosesJson = objectMapper.writeValueAsString(List.of(
                Map.of("diagnosisId", diagnosis1.getId(), "weight", 2.0),
                Map.of("diagnosisId", diagnosis2.getId(), "weight", 1.0)
        ));

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "sample.png",
                MediaType.IMAGE_PNG_VALUE,
                "fakepngcontent".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile findingsPart = new MockMultipartFile(
                "findings",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                findingsJson.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile diagnosesPart = new MockMultipartFile(
                "diagnoses",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                diagnosesJson.getBytes(StandardCharsets.UTF_8)
        );

        String createdBody = mockMvc.perform(multipart("/api/v1/admin/cases")
                        .file(imageFile)
                        .file(findingsPart)
                        .file(diagnosesPart)
                        .param("title", "Admin Case")
                        .param("description", "desc")
                        .param("modality", Modality.XRAY.name())
                        .param("species", Species.DOG.name())
                        .param("lesionCx", "0.4")
                        .param("lesionCy", "0.6")
                        .param("lesionR", "0.2")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.lesionData.cx").value(0.4))
                .andExpect(jsonPath("$.lesionDataJson").value(org.hamcrest.Matchers.containsString("CIRCLE")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdNode = objectMapper.readTree(createdBody);
        long caseId = createdNode.get("id").asLong();
        long initialVersion = createdNode.get("version").asLong();

        // Update with different answer configuration to bump version
        MockHttpServletRequestBuilder updateRequest = multipart("/api/v1/admin/cases/{id}", caseId)
                .file(imageFile)
                .file(findingsPart)
                .file(new MockMultipartFile(
                        "diagnoses",
                        "",
                        MediaType.APPLICATION_JSON_VALUE,
                        objectMapper.writeValueAsBytes(List.of(
                                Map.of("diagnosisId", diagnosis1.getId(), "weight", 3.0)
                        ))
                ))
                .param("title", "Admin Case Updated")
                .param("description", "desc")
                .param("modality", Modality.XRAY.name())
                .param("species", Species.DOG.name())
                .param("lesionCx", "0.5")
                .param("lesionCy", "0.5")
                .param("lesionR", "0.25")
                .header("Authorization", bearer(token))
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });

        String updatedBody = mockMvc.perform(updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin Case Updated"))
                .andExpect(jsonPath("$.findings.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long updatedVersion = objectMapper.readTree(updatedBody).get("version").asLong();
        assertThat(updatedVersion).isEqualTo(initialVersion + 1);

        mockMvc.perform(get("/api/v1/admin/cases")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(caseId));
    }

    @Test
    void updateCaseIsIdempotentAndReplacesAssociations() throws Exception {
        String token = createUserAndLogin(Role.ADMIN);
        Finding finding1 = findingRepository.save(new Finding("Opacity", ""));
        Finding finding2 = findingRepository.save(new Finding("Nodule", ""));
        Diagnosis diagnosis1 = diagnosisRepository.save(new Diagnosis("Pneumonia", ""));
        Diagnosis diagnosis2 = diagnosisRepository.save(new Diagnosis("Fracture", ""));

        String initialFindings = objectMapper.writeValueAsString(List.of(
                Map.of("findingId", finding1.getId(), "required", true)
        ));
        String initialDiagnoses = objectMapper.writeValueAsString(List.of(
                Map.of("diagnosisId", diagnosis1.getId(), "weight", 1.0)
        ));

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "sample.png",
                MediaType.IMAGE_PNG_VALUE,
                "fakepngcontent".getBytes(StandardCharsets.UTF_8)
        );

        long caseId = createCase(token, initialFindings, initialDiagnoses, imageFile);

        MockMultipartFile findingsPart = new MockMultipartFile(
                "findings",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                initialFindings.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile diagnosesPart = new MockMultipartFile(
                "diagnoses",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                initialDiagnoses.getBytes(StandardCharsets.UTF_8)
        );

        MockHttpServletRequestBuilder updateSamePayload = buildUpdateRequest(caseId, token, findingsPart, diagnosesPart);
        mockMvc.perform(updateSamePayload).andExpect(status().isOk());
        mockMvc.perform(updateSamePayload).andExpect(status().isOk());

        assertThat(caseFindingRepository.count()).isEqualTo(1);
        assertThat(caseDiagnosisRepository.count()).isEqualTo(1);

        String expandedFindings = objectMapper.writeValueAsString(List.of(
                Map.of("findingId", finding1.getId(), "required", true),
                Map.of("findingId", finding2.getId(), "required", false)
        ));
        String expandedDiagnoses = objectMapper.writeValueAsString(List.of(
                Map.of("diagnosisId", diagnosis1.getId(), "weight", 2.0),
                Map.of("diagnosisId", diagnosis2.getId(), "weight", 1.0)
        ));

        MockHttpServletRequestBuilder updateExpandedPayload = buildUpdateRequest(
                caseId,
                token,
                new MockMultipartFile(
                        "findings",
                        "",
                        MediaType.APPLICATION_JSON_VALUE,
                        expandedFindings.getBytes(StandardCharsets.UTF_8)
                ),
                new MockMultipartFile(
                        "diagnoses",
                        "",
                        MediaType.APPLICATION_JSON_VALUE,
                        expandedDiagnoses.getBytes(StandardCharsets.UTF_8)
                )
        );

        mockMvc.perform(updateExpandedPayload).andExpect(status().isOk());
        mockMvc.perform(updateExpandedPayload).andExpect(status().isOk());

        assertThat(caseFindingRepository.count()).isEqualTo(2);
        assertThat(caseDiagnosisRepository.count()).isEqualTo(2);
    }

    private long createCase(String token, String findingsJson, String diagnosesJson, MockMultipartFile imageFile) throws Exception {
        MockMultipartFile findingsPart = new MockMultipartFile(
                "findings",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                findingsJson.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile diagnosesPart = new MockMultipartFile(
                "diagnoses",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                diagnosesJson.getBytes(StandardCharsets.UTF_8)
        );

        String createdBody = mockMvc.perform(multipart("/api/v1/admin/cases")
                        .file(imageFile)
                        .file(findingsPart)
                        .file(diagnosesPart)
                        .param("title", "Admin Case")
                        .param("description", "desc")
                        .param("modality", Modality.XRAY.name())
                        .param("species", Species.DOG.name())
                        .param("lesionCx", "0.4")
                        .param("lesionCy", "0.6")
                        .param("lesionR", "0.2")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdNode = objectMapper.readTree(createdBody);
        return createdNode.get("id").asLong();
    }

    private MockHttpServletRequestBuilder buildUpdateRequest(
            long caseId,
            String token,
            MockMultipartFile findingsPart,
            MockMultipartFile diagnosesPart
    ) {
        return multipart("/api/v1/admin/cases/{id}", caseId)
                .file(findingsPart)
                .file(diagnosesPart)
                .param("title", "Admin Case")
                .param("description", "desc")
                .param("modality", Modality.XRAY.name())
                .param("species", Species.DOG.name())
                .param("lesionCx", "0.4")
                .param("lesionCy", "0.6")
                .param("lesionR", "0.2")
                .header("Authorization", bearer(token))
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
    }

    private String createUserAndLogin(Role role) throws Exception {
        String email = "user+" + role.name().toLowerCase() + "@example.com";
        String rawPassword = "password123";
        userRepository.save(new User(email, passwordEncoder.encode(rawPassword), "Tester", role));

        LoginRequest loginRequest = new LoginRequest(email, rawPassword);
        String loginResponse = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(loginResponse);
        return node.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
