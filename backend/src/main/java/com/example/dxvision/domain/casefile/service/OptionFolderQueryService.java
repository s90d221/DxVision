package com.example.dxvision.domain.casefile.service;

import com.example.dxvision.domain.casefile.Diagnosis;
import com.example.dxvision.domain.casefile.Finding;
import com.example.dxvision.domain.casefile.OptionFolder;
import com.example.dxvision.domain.casefile.OptionFolderType;
import com.example.dxvision.domain.casefile.dto.OptionFolderTreeResponse;
import com.example.dxvision.domain.repository.DiagnosisRepository;
import com.example.dxvision.domain.repository.FindingRepository;
import com.example.dxvision.domain.repository.OptionFolderRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OptionFolderQueryService {
    private final OptionFolderRepository folderRepository;
    private final FindingRepository findingRepository;
    private final DiagnosisRepository diagnosisRepository;

    public OptionFolderQueryService(
            OptionFolderRepository folderRepository,
            FindingRepository findingRepository,
            DiagnosisRepository diagnosisRepository
    ) {
        this.folderRepository = folderRepository;
        this.findingRepository = findingRepository;
        this.diagnosisRepository = diagnosisRepository;
    }

    public List<OptionFolderTreeResponse> getFindingTree() {
        return buildTree(OptionFolderType.FINDING);
    }

    public List<OptionFolderTreeResponse> getDiagnosisTree() {
        return buildTree(OptionFolderType.DIAGNOSIS);
    }

    private List<OptionFolderTreeResponse> buildTree(OptionFolderType type) {
        List<OptionFolderTreeResponse> tree = new ArrayList<>();
        List<OptionFolder> folders = folderRepository.findAllByTypeOrderByOrderIndexAsc(type);

        if (type == OptionFolderType.FINDING) {
            List<Finding> ungrouped = findingRepository.findByFolderIsNullOrderByOrderIndexAsc();
            for (OptionFolder folder : folders) {
                tree.add(new OptionFolderTreeResponse(
                        folder.getId(),
                        folder.getName(),
                        folder.getOrderIndex(),
                        findingRepository.findByFolderIdOrderByOrderIndexAsc(folder.getId())
                                .stream()
                                .map(f -> new OptionFolderTreeResponse.OptionItemResponse(f.getId(), f.getLabel(), f.getOrderIndex()))
                                .toList()
                ));
            }
            if (!ungrouped.isEmpty()) {
                tree.add(new OptionFolderTreeResponse(
                        null,
                        "Ungrouped",
                        Integer.MAX_VALUE,
                        ungrouped.stream()
                                .map(f -> new OptionFolderTreeResponse.OptionItemResponse(f.getId(), f.getLabel(), f.getOrderIndex()))
                                .toList()
                ));
            }
        } else {
            List<Diagnosis> ungrouped = diagnosisRepository.findByFolderIsNullOrderByOrderIndexAsc();
            for (OptionFolder folder : folders) {
                tree.add(new OptionFolderTreeResponse(
                        folder.getId(),
                        folder.getName(),
                        folder.getOrderIndex(),
                        diagnosisRepository.findByFolderIdOrderByOrderIndexAsc(folder.getId())
                                .stream()
                                .map(d -> new OptionFolderTreeResponse.OptionItemResponse(d.getId(), d.getName(), d.getOrderIndex()))
                                .toList()
                ));
            }
            if (!ungrouped.isEmpty()) {
                tree.add(new OptionFolderTreeResponse(
                        null,
                        "Ungrouped",
                        Integer.MAX_VALUE,
                        ungrouped.stream()
                                .map(d -> new OptionFolderTreeResponse.OptionItemResponse(d.getId(), d.getName(), d.getOrderIndex()))
                                .toList()
                ));
            }
        }

        return tree.stream()
                .sorted(Comparator.comparing(OptionFolderTreeResponse::orderIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }
}
