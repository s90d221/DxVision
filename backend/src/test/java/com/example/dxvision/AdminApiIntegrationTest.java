package com.example.dxvision;

import com.example.dxvision.domain.auth.Role;
import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.auth.dto.LoginRequest;
import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.ImageCaseRepository;
import com.example.dxvision.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private ImageCaseRepository imageCaseRepository;

    @BeforeEach
    void cleanDatabase() {
        imageCaseRepository.deleteAll();
        diagnosisRepository.deleteAll();
        findingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        String userToken = createUserAndLogin(Role.USER);

        mockMvc.perform(get("/api/v1/admin/findings")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCrudFindingsAndDiagnoses() throws Exception {
        String adminToken = createUserAndLogin(Role.ADMIN);

        // create finding
        String findingResponse = mockMvc.perform(post("/api/v1/admin/findings")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Cough","description":"Dry cough"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Cough"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long findingId = objectMapper.readTree(findingResponse).get("id").asLong();

        // create diagnosis
        String diagnosisResponse = mockMvc.perform(post("/api/v1/admin/diagnoses")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Flu","description":"desc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Flu"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long diagnosisId = objectMapper.readTree(diagnosisResponse).get("id").asLong();

        // update finding
        mockMvc.perform(put("/api/v1/admin/findings/{id}", findingId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Updated","description":"new"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Updated"));

        // update diagnosis
        mockMvc.perform(put("/api/v1/admin/diagnoses/{id}", diagnosisId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"UpdatedDx","description":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("UpdatedDx"));

        // list should include the updated entries
        mockMvc.perform(get("/api/v1/admin/findings")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(findingId));

        mockMvc.perform(get("/api/v1/admin/diagnoses")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(diagnosisId));

        // delete
        mockMvc.perform(delete("/api/v1/admin/findings/{id}", findingId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/admin/diagnoses/{id}", diagnosisId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanCreateCaseAndVersionIncrementsOnCorrectnessChange() throws Exception {
        String adminToken = createUserAndLogin(Role.ADMIN);
        Finding f1 = findingRepository.save(new Finding("F1", ""));
        Finding f2 = findingRepository.save(new Finding("F2", ""));
        Diagnosis d1 = diagnosisRepository.save(new Diagnosis("D1", ""));
        Diagnosis d2 = diagnosisRepository.save(new Diagnosis("D2", ""));

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Case A",
                "description", "desc",
                "modality", Modality.XRAY.name(),
                "species", Species.DOG.name(),
                "imageUrl", "http://example.com/a.jpg",
                "lesionShapeType", "CIRCLE",
                "lesionData", Map.of("cx", 0.5, "cy", 0.5, "r", 0.2),
                "findingOptionIds", List.of(f1.getId(), f2.getId()),
                "requiredFindingIds", List.of(f1.getId()),
                "diagnosisOptionWeights", List.of(
                        Map.of("diagnosisId", d1.getId(), "weight", 2.0),
                        Map.of("diagnosisId", d2.getId(), "weight", 1.0)
                )
        ));

        String created = mockMvc.perform(post("/api/v1/admin/cases")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.findings.length()").value(2))
                .andExpect(jsonPath("$.diagnoses.length()").value(2))
                .andExpect(jsonPath("$.lesionDataJson").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdNode = objectMapper.readTree(created);
        long caseId = createdNode.get("id").asLong();
        long initialVersion = createdNode.get("version").asLong();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title", "Case A",
                "description", "desc",
                "modality", Modality.XRAY.name(),
                "species", Species.DOG.name(),
                "imageUrl", "http://example.com/a.jpg",
                "lesionShapeType", "CIRCLE",
                "lesionData", Map.of("cx", 0.5, "cy", 0.5, "r", 0.25),
                "findingOptionIds", List.of(f1.getId(), f2.getId()),
                "requiredFindingIds", List.of(f1.getId(), f2.getId()),
                "diagnosisOptionWeights", List.of(
                        Map.of("diagnosisId", d1.getId(), "weight", 3.0)
                )
        ));

        String updated = mockMvc.perform(put("/api/v1/admin/cases/{id}", caseId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings.length()").value(2))
                .andExpect(jsonPath("$.diagnoses.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode updatedNode = objectMapper.readTree(updated);
        long updatedVersion = updatedNode.get("version").asLong();
        assertThat(updatedVersion).isEqualTo(initialVersion + 1);
    }

    @Test
    void creatingCaseWithInvalidRequiredFindingSubsetReturnsBadRequest() throws Exception {
        String adminToken = createUserAndLogin(Role.ADMIN);
        Finding f1 = findingRepository.save(new Finding("F1", ""));
        Finding f2 = findingRepository.save(new Finding("F2", ""));

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Bad Case",
                "description", "desc",
                "modality", Modality.XRAY.name(),
                "species", Species.DOG.name(),
                "imageUrl", "http://example.com/a.jpg",
                "lesionShapeType", "CIRCLE",
                "lesionData", Map.of("cx", 0.5, "cy", 0.5, "r", 0.2),
                "findingOptionIds", List.of(f1.getId()),
                "requiredFindingIds", List.of(f2.getId())
        ));

        mockMvc.perform(post("/api/v1/admin/cases")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("requiredFindingIds must be subset of findingOptionIds"));
    }

    private String createUserAndLogin(Role role) throws Exception {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String rawPassword = "Password123!";
        User user = new User(email, passwordEncoder.encode(rawPassword), "Test " + role.name(), role);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(email, rawPassword);
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(loginResponse).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
