package com.example.dxvision.domain.admin.controller;

import com.example.dxvision.domain.admin.dto.AdminCaseListItem;
import com.example.dxvision.domain.admin.dto.AdminCaseResponse;
import com.example.dxvision.domain.admin.dto.AdminCaseUpsertRequest;
import com.example.dxvision.domain.admin.dto.AdminDiagnosisWeight;
import com.example.dxvision.domain.admin.dto.AdminFindingSelection;
import com.example.dxvision.domain.admin.dto.LesionDataDto;
import com.example.dxvision.domain.admin.dto.PageResponse;
import com.example.dxvision.domain.admin.service.AdminCaseService;
import com.example.dxvision.domain.casefile.Modality;
import com.example.dxvision.domain.casefile.Species;
import com.example.dxvision.global.storage.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/cases")
public class ImageCaseAdminController {
    private final AdminCaseService adminCaseService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public ImageCaseAdminController(
            AdminCaseService adminCaseService,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper
    ) {
        this.adminCaseService = adminCaseService;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCaseResponse create(
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("modality") Modality modality,
            @RequestPart("species") Species species,
            @RequestPart("lesionCx") Double lesionCx,
            @RequestPart("lesionCy") Double lesionCy,
            @RequestPart(value = "lesionR", required = false) Double lesionR,
            @RequestPart(value = "findings", required = false) String findingsJson,
            @RequestPart(value = "diagnoses", required = false) String diagnosesJson,
            @RequestPart("image") MultipartFile image
    ) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is required");
        }

        String storedImageUrl = fileStorageService.store(image);
        try {
            AdminCaseUpsertRequest request = buildRequest(
                    title,
                    description,
                    modality,
                    species,
                    lesionCx,
                    lesionCy,
                    lesionR,
                    findingsJson,
                    diagnosesJson,
                    storedImageUrl
            );
            return adminCaseService.createCase(request);
        } catch (Exception ex) {
            fileStorageService.deleteIfLocal(storedImageUrl);
            throw ex;
        }
    }

    @PutMapping(value = "/{caseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminCaseResponse> update(
            @PathVariable Long caseId,
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("modality") Modality modality,
            @RequestPart("species") Species species,
            @RequestPart("lesionCx") Double lesionCx,
            @RequestPart("lesionCy") Double lesionCy,
            @RequestPart(value = "lesionR", required = false) Double lesionR,
            @RequestPart(value = "findings", required = false) String findingsJson,
            @RequestPart(value = "diagnoses", required = false) String diagnosesJson,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        String storedImageUrl = null;
        if (image != null && !image.isEmpty()) {
            storedImageUrl = fileStorageService.store(image);
        }
        try {
            AdminCaseUpsertRequest request = buildRequest(
                    title,
                    description,
                    modality,
                    species,
                    lesionCx,
                    lesionCy,
                    lesionR,
                    findingsJson,
                    diagnosesJson,
                    storedImageUrl
            );
            return ResponseEntity.ok(adminCaseService.updateCase(caseId, request));
        } catch (Exception ex) {
            if (storedImageUrl != null) {
                fileStorageService.deleteIfLocal(storedImageUrl);
            }
            throw ex;
        }
    }

    @GetMapping
    public PageResponse<AdminCaseListItem> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return adminCaseService.listCases(pageable);
    }

    @GetMapping("/{caseId}")
    public AdminCaseResponse get(@PathVariable Long caseId) {
        return adminCaseService.getCase(caseId);
    }

    @DeleteMapping("/{caseId}")
    public ResponseEntity<Void> delete(@PathVariable Long caseId) {
        adminCaseService.deleteCase(caseId);
        return ResponseEntity.noContent().build();
    }

    private AdminCaseUpsertRequest buildRequest(
            String title,
            String description,
            Modality modality,
            Species species,
            Double lesionCx,
            Double lesionCy,
            Double lesionR,
            String findingsJson,
            String diagnosesJson,
            String imageUrl
    ) {
        if (lesionCx == null || lesionCy == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesion coordinates are required");
        }

        double radius = lesionR == null ? 0.2 : lesionR;
        LesionDataDto lesionData = new LesionDataDto("CIRCLE", lesionCx, lesionCy, radius);
        List<AdminFindingSelection> findings = parseFindings(findingsJson);
        List<AdminDiagnosisWeight> diagnoses = parseDiagnoses(diagnosesJson);

        return new AdminCaseUpsertRequest(
                title,
                description,
                modality,
                species,
                imageUrl,
                lesionData,
                findings,
                diagnoses
        );
    }

    private List<AdminFindingSelection> parseFindings(String findingsJson) {
        if (!StringUtils.hasText(findingsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(findingsJson, new TypeReference<>() {});
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid findings payload");
        }
    }

    private List<AdminDiagnosisWeight> parseDiagnoses(String diagnosesJson) {
        if (!StringUtils.hasText(diagnosesJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(diagnosesJson, new TypeReference<>() {});
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid diagnoses payload");
        }
    }
}
