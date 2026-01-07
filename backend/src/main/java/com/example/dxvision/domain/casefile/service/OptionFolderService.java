package com.example.dxvision.domain.casefile.service;

import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.DiagnosisFolder;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.casefile.FindingFolder;
import com.example.dxvision.domain.casefile.OptionFolder;
import com.example.dxvision.domain.casefile.OptionType;
import com.example.dxvision.domain.casefile.dto.OptionFolderItemDto;
import com.example.dxvision.domain.casefile.dto.OptionFolderReorderRequest;
import com.example.dxvision.domain.casefile.dto.OptionFolderRequest;
import com.example.dxvision.domain.casefile.dto.OptionFolderResponse;
import com.example.dxvision.domain.repository.DiagnosisFolderRepository;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingFolderRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.OptionFolderRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OptionFolderService {
    private static final String DEFAULT_FOLDER_NAME = "Uncategorized";

    private final OptionFolderRepository optionFolderRepository;
    private final FindingRepository findingRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final FindingFolderRepository findingFolderRepository;
    private final DiagnosisFolderRepository diagnosisFolderRepository;

    public OptionFolderService(
            OptionFolderRepository optionFolderRepository,
            FindingRepository findingRepository,
            DiagnosisRepository diagnosisRepository,
            FindingFolderRepository findingFolderRepository,
            DiagnosisFolderRepository diagnosisFolderRepository
    ) {
        this.optionFolderRepository = optionFolderRepository;
        this.findingRepository = findingRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.findingFolderRepository = findingFolderRepository;
        this.diagnosisFolderRepository = diagnosisFolderRepository;
    }

    @PostConstruct
    public void ensureDefaultsOnBoot() {
        initializeDefaults();
    }

    @Transactional
    public void initializeDefaults() {
        for (OptionType type : OptionType.values()) {
            OptionFolder defaultFolder = optionFolderRepository.findByTypeAndSystemDefaultTrue(type)
                    .orElseGet(() -> createDefaultFolder(type));
            attachUnassignedItems(defaultFolder);
        }
    }

    @Transactional
    public OptionFolderResponse createFolder(OptionFolderRequest request) {
        OptionFolder folder = new OptionFolder(
                request.type(),
                request.name().trim(),
                resolveSortOrder(request.type(), request.sortOrder()),
                false
        );
        OptionFolder saved = optionFolderRepository.save(folder);
        return toResponse(saved, List.of());
    }

    @Transactional
    public OptionFolderResponse updateFolder(Long id, OptionFolderRequest request) {
        OptionFolder folder = optionFolderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        if (folder.getType() != request.type()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder type mismatch");
        }
        folder.update(request.name().trim(), request.sortOrder());
        return toResponse(folder, loadItems(folder, null));
    }

    @Transactional
    public void deleteFolder(Long id) {
        OptionFolder folder = optionFolderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        if (folder.isSystemDefault()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default folder cannot be deleted");
        }

        OptionFolder defaultFolder = optionFolderRepository.findByTypeAndSystemDefaultTrue(folder.getType())
                .orElseGet(() -> createDefaultFolder(folder.getType()));

        moveItemsToFolder(folder, defaultFolder);
        optionFolderRepository.delete(folder);
    }

    @Transactional
    public void reorderFolders(OptionFolderReorderRequest request) {
        Map<Long, Integer> orderMap = request.orders().stream()
                .filter(o -> o.id() != null && o.sortOrder() != null)
                .collect(Collectors.toMap(OptionFolderReorderRequest.FolderOrder::id, OptionFolderReorderRequest.FolderOrder::sortOrder));

        List<OptionFolder> folders = optionFolderRepository.findAllByTypeOrderBySortOrderAsc(request.type());
        for (OptionFolder folder : folders) {
            Integer next = orderMap.get(folder.getId());
            if (next != null) {
                folder.update(folder.getName(), next);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<OptionFolderResponse> listFoldersWithItems(OptionType type, Set<Long> allowedItemIds) {
        List<OptionFolder> folders = optionFolderRepository.findAllByTypeOrderBySortOrderAsc(type);
        List<OptionFolderResponse> responses = new ArrayList<>();
        for (OptionFolder folder : folders) {
            List<OptionFolderItemDto> items = loadItems(folder, allowedItemIds);
            if (!items.isEmpty() || allowedItemIds == null) {
                responses.add(toResponse(folder, items));
            }
        }
        return responses;
    }

    @Transactional
    public void syncFindingFolders(Finding finding, List<Long> folderIds) {
        Set<Long> desiredIds = normalizeFolderIds(folderIds, OptionType.FINDING);
        List<FindingFolder> current = findingFolderRepository.findOrderedByTypeAndFindingIds(OptionType.FINDING, List.of(finding.getId()));

        Map<Long, FindingFolder> currentByFolder = new HashMap<>();
        for (FindingFolder mapping : current) {
            currentByFolder.put(mapping.getFolder().getId(), mapping);
        }

        // remove
        for (FindingFolder mapping : current) {
            if (!desiredIds.contains(mapping.getFolder().getId())) {
                findingFolderRepository.delete(mapping);
            }
        }

        // add missing
        for (Long folderId : desiredIds) {
            if (!currentByFolder.containsKey(folderId)) {
                OptionFolder folder = optionFolderRepository.findById(folderId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
                int baseOrder = determineBaseOrder(folderId, true);
                FindingFolder mapping = new FindingFolder(folder, finding, baseOrder);
                findingFolderRepository.save(mapping);
            }
        }
    }

    @Transactional
    public void syncDiagnosisFolders(Diagnosis diagnosis, List<Long> folderIds) {
        Set<Long> desiredIds = normalizeFolderIds(folderIds, OptionType.DIAGNOSIS);
        List<DiagnosisFolder> current = diagnosisFolderRepository.findOrderedByTypeAndDiagnosisIds(
                OptionType.DIAGNOSIS, List.of(diagnosis.getId()));

        Map<Long, DiagnosisFolder> currentByFolder = new HashMap<>();
        for (DiagnosisFolder mapping : current) {
            currentByFolder.put(mapping.getFolder().getId(), mapping);
        }

        for (DiagnosisFolder mapping : current) {
            if (!desiredIds.contains(mapping.getFolder().getId())) {
                diagnosisFolderRepository.delete(mapping);
            }
        }

        for (Long folderId : desiredIds) {
            if (!currentByFolder.containsKey(folderId)) {
                OptionFolder folder = optionFolderRepository.findById(folderId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
                int baseOrder = determineBaseOrder(folderId, false);
                DiagnosisFolder mapping = new DiagnosisFolder(folder, diagnosis, baseOrder);
                diagnosisFolderRepository.save(mapping);
            }
        }
    }

    private OptionFolder createDefaultFolder(OptionType type) {
        Integer nextSort = resolveSortOrder(type, null);
        OptionFolder folder = new OptionFolder(type, DEFAULT_FOLDER_NAME, nextSort, true);
        folder.markAsDefault();
        return optionFolderRepository.save(folder);
    }

    private Integer resolveSortOrder(OptionType type, Integer requested) {
        if (requested != null) {
            return requested;
        }
        Integer max = optionFolderRepository.findMaxSortOrderByType(type);
        if (max == null) {
            return 0;
        }
        return max + 1;
    }

    private void attachUnassignedItems(OptionFolder defaultFolder) {
        if (defaultFolder.getType() == OptionType.FINDING) {
            List<Finding> findings = findingRepository.findAll();
            for (Finding finding : findings) {
                boolean hasMapping = findingFolderRepository.existsByFindingId(finding.getId());
                if (!hasMapping) {
                    findingFolderRepository.save(new FindingFolder(defaultFolder, finding, 0));
                }
            }
        } else if (defaultFolder.getType() == OptionType.DIAGNOSIS) {
            List<Diagnosis> diagnoses = diagnosisRepository.findAll();
            for (Diagnosis diagnosis : diagnoses) {
                boolean hasMapping = diagnosisFolderRepository.existsByDiagnosisId(diagnosis.getId());
                if (!hasMapping) {
                    diagnosisFolderRepository.save(new DiagnosisFolder(defaultFolder, diagnosis, 0));
                }
            }
        }
    }

    private List<OptionFolderItemDto> loadItems(OptionFolder folder, Set<Long> allowedItemIds) {
        if (folder.getType() == OptionType.FINDING) {
            List<FindingFolder> mappings = findingFolderRepository.findByFolderId(folder.getId());
            return mappings.stream()
                    .filter(ff -> allowedItemIds == null || allowedItemIds.contains(ff.getFinding().getId()))
                    .sorted(Comparator.comparing(FindingFolder::getSortOrder))
                    .map(ff -> new OptionFolderItemDto(
                            ff.getFinding().getId(),
                            ff.getFinding().getLabel(),
                            ff.getFinding().getDescription(),
                            ff.getSortOrder()
                    ))
                    .toList();
        }

        List<DiagnosisFolder> mappings = diagnosisFolderRepository.findByFolderId(folder.getId());
        return mappings.stream()
                .filter(df -> allowedItemIds == null || allowedItemIds.contains(df.getDiagnosis().getId()))
                .sorted(Comparator.comparing(DiagnosisFolder::getSortOrder))
                .map(df -> new OptionFolderItemDto(
                        df.getDiagnosis().getId(),
                        df.getDiagnosis().getName(),
                        df.getDiagnosis().getDescription(),
                        df.getSortOrder()
                ))
                .toList();
    }

    private void moveItemsToFolder(OptionFolder source, OptionFolder target) {
        if (source.getType() == OptionType.FINDING) {
            List<FindingFolder> mappings = findingFolderRepository.findByFolderId(source.getId());
            int baseOrder = determineBaseOrder(target.getId(), true);
            for (FindingFolder mapping : mappings) {
                findingFolderRepository.delete(mapping);
                findingFolderRepository.save(new FindingFolder(target, mapping.getFinding(), baseOrder++));
            }
        } else if (source.getType() == OptionType.DIAGNOSIS) {
            List<DiagnosisFolder> mappings = diagnosisFolderRepository.findByFolderId(source.getId());
            int baseOrder = determineBaseOrder(target.getId(), false);
            for (DiagnosisFolder mapping : mappings) {
                diagnosisFolderRepository.delete(mapping);
                diagnosisFolderRepository.save(new DiagnosisFolder(target, mapping.getDiagnosis(), baseOrder++));
            }
        }
    }

    private int determineBaseOrder(Long folderId, boolean finding) {
        Integer max = finding
                ? findingFolderRepository.findMaxSortOrderByFolderId(folderId)
                : diagnosisFolderRepository.findMaxSortOrderByFolderId(folderId);
        return max == null ? 0 : max + 1;
    }

    private OptionFolderResponse toResponse(OptionFolder folder, List<OptionFolderItemDto> items) {
        return new OptionFolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getType(),
                folder.getSortOrder(),
                folder.isSystemDefault(),
                items
        );
    }

    private Set<Long> normalizeFolderIds(List<Long> folderIds, OptionType type) {
        Set<Long> normalized = new HashSet<>();
        if (folderIds != null) {
            for (Long id : folderIds) {
                if (id != null) {
                    normalized.add(id);
                }
            }
        }
        if (normalized.isEmpty()) {
            OptionFolder defaultFolder = optionFolderRepository.findByTypeAndSystemDefaultTrue(type)
                    .orElseGet(() -> createDefaultFolder(type));
            normalized.add(defaultFolder.getId());
        } else {
            // Validate all
            List<OptionFolder> folders = optionFolderRepository.findAllById(normalized);
            if (folders.size() != normalized.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder not found");
            }
            for (OptionFolder folder : folders) {
                if (folder.getType() != type) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder type mismatch");
                }
            }
        }
        return normalized;
    }
}
