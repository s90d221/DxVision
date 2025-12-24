package com.example.dxvision.global.dev;

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
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalSeedDataRunner implements CommandLineRunner {

    private final FindingRepository findingRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final ImageCaseRepository imageCaseRepository;

    public LocalSeedDataRunner(
            FindingRepository findingRepository,
            DiagnosisRepository diagnosisRepository,
            ImageCaseRepository imageCaseRepository
    ) {
        this.findingRepository = findingRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.imageCaseRepository = imageCaseRepository;
    }

    @Override
    public void run(String... args) {
        if (imageCaseRepository.count() > 0) {
            return;
        }

        List<Finding> findings = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            findings.add(findingRepository.save(new Finding("Finding " + i, "Seed finding " + i)));
        }

        List<Diagnosis> diagnoses = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            diagnoses.add(diagnosisRepository.save(new Diagnosis("Diagnosis " + i, "Seed diagnosis " + i)));
        }

        for (int i = 1; i <= 5; i++) {
            ImageCase imageCase = new ImageCase(
                    "Seed Case " + i,
                    "Example case " + i,
                    i % 2 == 0 ? Modality.ULTRASOUND : Modality.XRAY,
                    i % 2 == 0 ? Species.CAT : Species.DOG,
                    "https://placehold.co/800x600?text=Case+" + i,
                    LesionShapeType.CIRCLE,
                    """
                    {"type":"CIRCLE","cx":0.5,"cy":0.5,"r":0.2}
                    """
            );

            imageCase.getFindings().add(new CaseFinding(imageCase, findings.get((i - 1) % findings.size()), true));
            imageCase.getDiagnoses().add(new CaseDiagnosis(imageCase, diagnoses.get((i - 1) % diagnoses.size()), 1.0));

            imageCaseRepository.save(imageCase);
        }
    }
}
