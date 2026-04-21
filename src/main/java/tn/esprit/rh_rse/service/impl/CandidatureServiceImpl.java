package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.ChangerStatutRequest;
import tn.esprit.rh_rse.dto.response.CandidatureResponse;
import tn.esprit.rh_rse.entity.Candidature;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;
import tn.esprit.rh_rse.exception.RecrutementNotFoundException;
import tn.esprit.rh_rse.repository.CandidatureRepository;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.repository.EntretienRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.CandidatureService;
import tn.esprit.rh_rse.service.EmailService;
import tn.esprit.rh_rse.service.QrCodeService;
import tn.esprit.rh_rse.dto.response.CVAiDTO;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final RestTemplate restTemplate;

    private static final String PYTHON_AI_URL = "http://localhost:5000/extract-profile";
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d{1,2})\\s*(ans?|years?)", Pattern.CASE_INSENSITIVE);

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

        // --- ANALYSE IA RÉELLE (Appel API Python) ---
        CVAiDTO aiResult = analyserCVAvecIA(cv);
        
        // --- VALIDATION STRICTE ---
        if (aiResult == null || aiResult.getProfile() == null) {
            throw new RuntimeException("L'analyse IA a échoué. Veuillez soumettre un CV valide au format PDF.");
        }

        CVAiDTO.ProfileDTO profile = aiResult.getProfile();
        
        // Blocage si informations critiques manquantes (Signe d'un CV vide ou fictif)
        if (profile.getNomComplet() == null || profile.getEmail() == null) {
            throw new RuntimeException("Impossible d'identifier le candidat (Nom/Email manquant). Votre CV semble incomplet ou illisible.");
        }

        // Blocage si aucune compétence détectée
        if (profile.getSkills() == null || profile.getSkills().isEmpty()) {
            throw new RuntimeException("Aucune compétence technique n'a été détectée dans votre CV. Postulation refusée car le profil ne correspond pas.");
        }

        // Mise à jour de la candidature avec les données réelles de l'IA
        candidature.setCompetencesExtraites(profile.getSkills());
        candidature.setAnneesExperienceDetecte(profile.getAnneesExperience() != null ? profile.getAnneesExperience() : 0);

        // Calcul du score de matching réel
        executerAnalyseIA(candidature, offre, profile);

        // Blocage si score trop bas
        if (candidature.getScoreMatching() < 5.0) {
            throw new RuntimeException("Votre profil ne correspond pas aux exigences minimales de cette offre (Score matching < 5%).");
        }

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
        
        // Si le score est inférieur à 60%, on exige une formation (cours d'anglais)
        if (scoreLangue != null && scoreLangue < 60.0) {
            c.setFormationRequise(true);
        } else {
            c.setFormationRequise(false);
        }
        
        return toResponse(candidatureRepository.save(c));
    }

    @Override
    public CandidatureResponse modifierCandidature(String id, MultipartFile cv, MultipartFile lettre) {
        // Logique de modification ...
        return getCandidatureById(id);
    }

    @Override
    public byte[] genererContratPdf(String candidatureId) {
        Candidature c = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new RecrutementNotFoundException("Candidature introuvable"));
        Offre o = offreRepository.findById(c.getOffreId())
                .orElseThrow(() -> new RecrutementNotFoundException("Offre introuvable"));
        User user = userRepository.findById(c.getCandidatId())
                .orElseThrow(() -> new RecrutementNotFoundException("Candidat introuvable"));

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("CONTRAT DE TRAVAIL - " + o.getTitre());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.setLeading(15f);
                contentStream.newLineAtOffset(50, 700);

                contentStream.showText("Entre l'entreprise RH_RSE et :");
                contentStream.newLine();
                contentStream.showText("M./Mme " + user.getNom() + " " + user.getPrenom());
                contentStream.newLine();
                contentStream.showText("Email : " + user.getEmail());
                contentStream.newLine();
                contentStream.newLine();
                contentStream.showText("Il a été convenu ce qui suit :");
                contentStream.newLine();
                contentStream.showText("1. Poste : " + o.getTitre());
                contentStream.newLine();
                contentStream.showText("2. Lieu de travail : " + o.getLieu());
                contentStream.newLine();
                contentStream.showText("3. Type de contrat : " + o.getTypeContrat());
                contentStream.newLine();
                contentStream.newLine();
                contentStream.showText("Fait à Tunis, le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Erreur génération contrat PDF: {}", e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public byte[] genererCoachTipsPdf(String tipsText) {
        String cleanTips = cleanTextForPdf(tipsText);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Header
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 22);
                contentStream.setNonStrokingColor(63, 81, 181); // Indigo
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Mes Conseils Career Coach IA");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.setNonStrokingColor(100, 100, 100);
                contentStream.newLineAtOffset(50, 730);
                contentStream.showText("Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                contentStream.endText();

                // Separator
                contentStream.setLineWidth(1f);
                contentStream.moveTo(50, 715);
                contentStream.lineTo(550, 715);
                contentStream.stroke();

                // Content
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.setNonStrokingColor(0, 0, 0);
                contentStream.setLeading(18f);
                contentStream.newLineAtOffset(50, 680);

                String[] lines = cleanTips.split("\n");
                for (String line : lines) {
                    String sanitizedLine = line.trim();
                    if (sanitizedLine.isEmpty()) {
                        contentStream.newLine();
                    } else {
                        drawWrappedText(contentStream, sanitizedLine, 500, PDType1Font.HELVETICA, 11);
                    }
                }
                contentStream.endText();
                
                // Footer
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                contentStream.setNonStrokingColor(150, 150, 150);
                contentStream.newLineAtOffset(220, 50);
                contentStream.showText("© RH_RSE - Plateforme de Recrutement Innovante");
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur critique génération Tips PDF: {}", e.getMessage());
            return new byte[0];
        }
    }

    private String cleanTextForPdf(String text) {
        if (text == null) return "";
        // Supprime les emojis et caractères spéciaux non supportés par Helvetica standard
        // On ne garde que les lettres (y compris accentuées), chiffres, ponctuation de base
        return text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}\\s\\n\\r]", " ")
                   .replace("💡", "[ASTUCE]")
                   .replace("🎓", "[FORMATION]")
                   .replace("🚨", "[ALERTE]")
                   .replace("✅", "[OK]");
    }

    private void drawWrappedText(PDPageContentStream contentStream, String text, float width, PDType1Font font, int fontSize) throws IOException {
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            
            String testLine = line.length() == 0 ? word : line + " " + word;
            float lineWidth = fontSize * font.getStringWidth(testLine) / 1000;
            
            if (lineWidth > width && line.length() > 0) {
                contentStream.showText(line.toString());
                contentStream.newLine();
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        
        if (line.length() > 0) {
            contentStream.showText(line.toString());
            contentStream.newLine();
        }
    }

    private CVAiDTO analyserCVAvecIA(MultipartFile cv) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("cv", cv.getResource());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<CVAiDTO> response = restTemplate.postForEntity(PYTHON_AI_URL, requestEntity, CVAiDTO.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à l'API IA Python : {}", e.getMessage());
            return null;
        }
    }

    private void executerAnalyseIA(Candidature candidature, Offre offre, CVAiDTO.ProfileDTO profile) {
        List<String> requiredSkills = offre.getCompetencesRequises() == null ? List.of() : offre.getCompetencesRequises();
        List<String> extractedSkills = profile.getSkills() == null ? List.of() : profile.getSkills();

        List<String> missingSkills = requiredSkills.stream()
                .filter(req -> extractedSkills.stream().noneMatch(ext -> normalize(ext).contains(normalize(req))))
                .collect(Collectors.toList());

        int matchedCount = requiredSkills.size() - missingSkills.size();
        double skillCoverage = requiredSkills.isEmpty() ? 100.0 : (matchedCount * 100.0) / requiredSkills.size();
        
        int detectedYears = profile.getAnneesExperience() != null ? profile.getAnneesExperience() : 0;
        double experienceScore = scoreExperience(detectedYears, offre.getNiveauExperience());
        
        double finalScore = (skillCoverage * 0.8) + (experienceScore * 0.2);
        finalScore = Math.max(0.0, Math.min(100.0, finalScore));

        candidature.setCompetencesManquantes(missingSkills);
        candidature.setScoreMatching(Math.round(finalScore * 10.0) / 10.0);
        candidature.setFormationRequise(!missingSkills.isEmpty());
        candidature.setComparaisonExplication(buildComparisonExplanation(requiredSkills, matchedCount, skillCoverage, detectedYears, experienceScore, finalScore, missingSkills));
    }

    private String extractTextFromPdf(MultipartFile cv) {
        if (cv == null || cv.isEmpty()) {
            return "";
        }
        try (PDDocument document = PDDocument.load(cv.getInputStream())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            return textStripper.getText(document);
        } catch (IOException e) {
            log.warn("Impossible de lire le CV PDF pour l'analyse IA: {}", e.getMessage());
            return "";
        }
    }

    private boolean containsSkill(String normalizedCv, String skill) {
        String normalizedSkill = normalize(skill);
        if (normalizedSkill.isBlank()) {
            return false;
        }
        return normalizedCv.contains(normalizedSkill);
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase()
                .replaceAll("[^\\p{L}\\p{Nd}\\s+#.]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int detectYearsOfExperience(String cvText) {
        if (cvText == null || cvText.isBlank()) {
            return 0;
        }
        Matcher matcher = EXPERIENCE_PATTERN.matcher(cvText.toLowerCase());
        int maxYears = 0;
        while (matcher.find()) {
            int years = Integer.parseInt(matcher.group(1));
            if (years > maxYears) {
                maxYears = years;
            }
        }
        return maxYears;
    }

    private double scoreExperience(int detectedYears, String expectedLevel) {
        int expectedYears = expectedYearsForLevel(expectedLevel);
        if (expectedYears <= 0) {
            return detectedYears > 0 ? 70.0 : 50.0;
        }
        if (detectedYears >= expectedYears) {
            return 100.0;
        }
        return (detectedYears * 100.0) / expectedYears;
    }

    private int expectedYearsForLevel(String niveauExperience) {
        if (niveauExperience == null || niveauExperience.isBlank()) {
            return 0;
        }
        String normalized = niveauExperience.toLowerCase();
        Matcher matcher = Pattern.compile("(\\d{1,2})").matcher(normalized);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        if (normalized.contains("junior") || normalized.contains("debutant")) {
            return 1;
        }
        if (normalized.contains("intermediaire") || normalized.contains("intermédiaire")) {
            return 3;
        }
        if (normalized.contains("senior") || normalized.contains("confirme") || normalized.contains("confirmé")) {
            return 5;
        }
        return 0;
    }

    private String buildComparisonExplanation(List<String> requiredSkills,
                                              int matchedCount,
                                              double skillCoverage,
                                              int detectedYears,
                                              double experienceScore,
                                              double finalScore,
                                              List<String> missingSkills) {
        String requirementsPart = requiredSkills.isEmpty()
                ? "Aucune compétence obligatoire n'a été définie pour cette offre."
                : String.format("%d/%d compétences requises détectées (%.1f%%).", matchedCount, requiredSkills.size(), skillCoverage);

        String experiencePart = String.format("Expérience détectée: %d an(s), score expérience %.1f%%.", detectedYears, experienceScore);

        String missingPart = missingSkills.isEmpty()
                ? "Aucune compétence manquante critique."
                : "Compétences manquantes: " + String.join(", ", missingSkills) + ".";

        return String.format("%s %s %s Score final: %.1f/100 (80%% compétences, 20%% expérience).",
                requirementsPart, experiencePart, missingPart, finalScore);
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
                .cvFileId(c.getCvFileId()).lettreMotivationFileId(c.getLettreMotivationFileId())
                .historiqueStatuts(c.getHistoriqueStatuts())
                .competencesExtraites(c.getCompetencesExtraites())
                .competencesManquantes(c.getCompetencesManquantes())
                .comparaisonExplication(c.getComparaisonExplication())
                .anneesExperienceDetecte(c.getAnneesExperienceDetecte())
                .testLanguePasse(c.getTestLanguePasse()).scoreLangue(c.getScoreLangue())
                .formationRequise(c.getFormationRequise())
                .scoreLeadership(c.getScoreLeadership()).scoreEmpathie(c.getScoreEmpathie())
                .scoreAdaptabilite(c.getScoreAdaptabilite()).scoreCommunication(c.getScoreCommunication())
                .scoreInnovation(c.getScoreInnovation())
                .datePostulation(c.getDatePostulation()).dateDerniereMAJ(c.getDateDerniereMAJ())
                .build();
    }
}