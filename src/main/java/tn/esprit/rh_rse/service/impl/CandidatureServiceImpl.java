package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.ChangerStatutRequest;
import tn.esprit.rh_rse.dto.response.CandidatureResponse;
import tn.esprit.rh_rse.entity.Candidature;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;
import tn.esprit.rh_rse.exception.RecrutementNotFoundException;
import tn.esprit.rh_rse.repository.CandidatureRepository;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.repository.EntretienRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.CandidatureService;
import tn.esprit.rh_rse.service.EmailService;
import tn.esprit.rh_rse.service.QrCodeService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidatureServiceImpl implements CandidatureService {

    private final EmailService emailService;
    private final QrCodeService qrCodeService;
    private final CandidatureRepository candidatureRepository;
    private final OffreRepository offreRepository;
    private final UserRepository userRepository;
    private final GridFsTemplate gridFsTemplate;
    private final EntretienRepository entretienRepository;

    private static final List<String> SKILLS_LIBRARY = List.of(
            "Java", "Spring Boot", "Angular", "React", "Vue.js", "Python", "Docker", "Kubernetes", "AWS", "Azure",
            "SQL", "NoSQL", "MongoDB", "PostgreSQL", "JavaScript", "TypeScript", "Node.js", "C#", "PHP", "Laravel",
            "Agile", "Scrum", "DevOps", "CI/CD", "Git", "Machine Learning", "Intelligence Artificielle", "Data Science",
            "NLP", "Big Data", "Spark", "Hadoop", "Management", "Leadership", "Communication", "Gestion de projet",
            "Jira", "Linux", "Cybersecurité", "Networking", "QA Testing", "Selenium", "Mobile Development", "Flutter",
            "React Native", "Android", "iOS", "Swift", "Kotlin", "C++", "HTML", "CSS", "UI/UX Design", "Figma"
    );

    @Override
    public CandidatureResponse postuler(String candidatId, String offreId,
                                        MultipartFile cv, MultipartFile lettre) {

        Offre offre = offreRepository.findById(offreId)
                .orElseThrow(() -> new RecrutementNotFoundException("Offre introuvable : " + offreId));

        candidatureRepository.findByCandidatIdAndOffreId(candidatId, offreId).ifPresent(c -> {
            throw new RuntimeException("Vous avez déjà postulé à cette offre.");
        });

        String cvFileId = null;
        if (cv != null && !cv.isEmpty()) {
            try {
                cvFileId = gridFsTemplate.store(cv.getInputStream(), "cv_" + candidatId + "_" + offreId + ".pdf", cv.getContentType()).toString();
            } catch (IOException e) {
                throw new RuntimeException("Erreur upload CV : " + e.getMessage());
            }
        }

        String lettreFileId = null;
        if (lettre != null && !lettre.isEmpty()) {
            try {
                lettreFileId = gridFsTemplate.store(lettre.getInputStream(), "lettre_" + candidatId + "_" + offreId + ".pdf", lettre.getContentType()).toString();
            } catch (IOException e) {
                throw new RuntimeException("Erreur upload lettre : " + e.getMessage());
            }
        }

        // Création de l'objet
        Candidature candidature = new Candidature();
        candidature.setCandidatId(candidatId);
        candidature.setOffreId(offreId);
        candidature.setCvFileId(cvFileId);
        candidature.setLettreMotivationFileId(lettreFileId);
        candidature.setStatut(StatutCandidature.NOUVEAU);
        candidature.setEtapeActuelle(labelEtape(StatutCandidature.NOUVEAU));
        candidature.setDatePostulation(LocalDateTime.now());
        candidature.setDateDerniereMAJ(LocalDateTime.now());
        candidature.setHistoriqueStatuts(new ArrayList<>());

        executerAnalyseIA(candidature, offre, cv);

        Candidature saved = candidatureRepository.save(candidature);

        try {
            userRepository.findById(candidatId).ifPresent(u -> {
                emailService.envoyerConfirmationCandidature(u.getEmail(), u.getPrenom() + " " + u.getNom(), offre.getTitre());
            });
        } catch (Exception e) {
            log.warn("Erreur envoi email : {}", e.getMessage());
        }

        offre.setNombreCandidatures(offre.getNombreCandidatures() + 1);
        offreRepository.save(offre);

        return toResponse(saved);
    }

    @Override
    public CandidatureResponse ajouterNotesRecruteur(String id, String notes) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Candidature introuvable : " + id));
        c.setNotesRecruteur(notes);
        c.setDateDerniereMAJ(LocalDateTime.now());
        return toResponse(candidatureRepository.save(c));
    }

    @Override
    public Map<StatutCandidature, List<CandidatureResponse>> getKanban(String offreId) {
        return candidatureRepository.findByOffreId(offreId).stream()
                .collect(Collectors.groupingBy(Candidature::getStatut,
                        Collectors.mapping(this::toResponse, Collectors.toList())));
    }

    @Override
    public List<CandidatureResponse> getTopCandidatsByScore(String offreId) {
        return candidatureRepository.findByOffreIdOrderByScoreMatchingDesc(offreId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CandidatureResponse changerStatut(String id, ChangerStatutRequest request) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Candidature introuvable : " + id));

        c.setStatut(request.getNouveauStatut());
        c.setEtapeActuelle(labelEtape(request.getNouveauStatut()));
        if (c.getHistoriqueStatuts() == null) c.setHistoriqueStatuts(new ArrayList<>());
        c.getHistoriqueStatuts().add(request.getNouveauStatut().name() + " - " + LocalDateTime.now());
        c.setDateDerniereMAJ(LocalDateTime.now());

        Candidature saved = candidatureRepository.save(c);
        // ... Logique Email Acceptation/QR Code (omise ici pour brièveté mais à garder de votre code initial) ...
        return toResponse(saved);
    }

    @Override
    public List<CandidatureResponse> getCandidaturesParOffre(String offreId) {
        return candidatureRepository.findByOffreId(offreId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<CandidatureResponse> getMesCandidatures(String candidatId) {
        return candidatureRepository.findByCandidatId(candidatId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CandidatureResponse getCandidatureById(String id) {
        return toResponse(candidatureRepository.findById(id).orElseThrow(() -> new RecrutementNotFoundException("Introuvable")));
    }

    @Override
    public void deleteCandidature(String id) {
        candidatureRepository.deleteById(id);
    }

    @Override
    public CandidatureResponse soumettreTestLangue(String candidatureId, Double scoreLangue) {
        Candidature c = candidatureRepository.findById(candidatureId).orElseThrow(() -> new RecrutementNotFoundException("Introuvable"));
        c.setScoreLangue(scoreLangue);
        c.setTestLanguePasse(true);
        return toResponse(candidatureRepository.save(c));
    }

    @Override
    public CandidatureResponse modifierCandidature(String id, MultipartFile cv, MultipartFile lettre) {
        // Logique de modification ...
        return getCandidatureById(id);
    }

    @Override
    public byte[] genererContratPdf(String candidatureId) {
        // Votre logique PDFBox existante ici
        return new byte[0];
    }

    @Override
    public byte[] genererCoachTipsPdf(String tipsText) {
        // Votre logique PDFBox existante ici
        return new byte[0];
    }

    private void executerAnalyseIA(Candidature candidature, Offre offre, MultipartFile cv) {
        // Logique IA existante ...
        candidature.setScoreMatching(75.0); // Exemple
    }

    private String labelEtape(StatutCandidature statut) {
        return switch (statut) {
            case NOUVEAU -> "CV Reçu";
            case EN_COURS_ANALYSE -> "Analyse";
            case ENTRETIEN_RH -> "Entretien RH";
            case ENTRETIEN_TECHNIQUE -> "Entretien Tech";
            case TEST_TECHNIQUE -> "Test Tech";
            case OFFRE_ENVOYEE -> "Offre envoyée";
            case ACCEPTE -> "Accepté";
            case REFUSE -> "Refusé";
        };
    }

    private CandidatureResponse toResponse(Candidature c) {
        return CandidatureResponse.builder()
                .id(c.getId()).candidatId(c.getCandidatId()).offreId(c.getOffreId())
                .statut(c.getStatut()).etapeActuelle(c.getEtapeActuelle())
                .scoreMatching(c.getScoreMatching()).notesRecruteur(c.getNotesRecruteur())
                .build();
    }
}