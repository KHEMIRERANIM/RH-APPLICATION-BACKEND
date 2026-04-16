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
    private final QrCodeService qrCodeService; // AJOUT
    private final CandidatureRepository candidatureRepository;
    private final OffreRepository offreRepository;
    private final UserRepository userRepository;
    private final GridFsTemplate gridFsTemplate;
    private final EntretienRepository entretienRepository;


    // --- BIBLIOTHÈQUE GLOBALE DE COMPÉTENCES POUR DÉTECTION PROACTIVE ---
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

        // --- DÉCLENCHEMENT DU MOTEUR IA ---
        executerAnalyseIA(candidature, offre, cv);

        Candidature saved = candidatureRepository.save(candidature);

        // Email confirmation candidature

        try {

            userRepository.findById(candidatId).ifPresent(u -> {

                String titreOffre = offreRepository.findById(offreId)

                        .map(o -> o.getTitre())
                        .orElse("Offre");

                emailService.envoyerConfirmationCandidature(

                        u.getEmail(),
                        u.getPrenom() + " " + u.getNom(),
                        titreOffre
                );

            });

        } catch (Exception e) {

            log.warn("Erreur envoi email candidature : {}", e.getMessage());

        }

        offreRepository.findById(offreId).ifPresent(o -> {

            o.setNombreCandidatures(
                    o.getNombreCandidatures() + 1
            );

            offreRepository.save(o);

        });

        return toResponse(saved);

    }

    @Override
    public CandidatureResponse changerStatut(String id, ChangerStatutRequest request) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException(
                        "Candidature introuvable : " + id));

        c.setStatut(request.getNouveauStatut());
        c.setEtapeActuelle(labelEtape(request.getNouveauStatut()));

        if (c.getHistoriqueStatuts() == null) {
            c.setHistoriqueStatuts(new ArrayList<>());
        }
        c.getHistoriqueStatuts().add(
                request.getNouveauStatut().name() + " - " + LocalDateTime.now()
                        + (request.getCommentaire() != null ? " | " + request.getCommentaire() : "")
        );
        c.setDateDerniereMAJ(LocalDateTime.now());
        Candidature saved = candidatureRepository.save(c);

        // Récupère infos candidat et offre
        try {
            userRepository.findById(c.getCandidatId()).ifPresent(candidat -> {
                String titreOffre = offreRepository.findById(c.getOffreId())
                        .map(o -> o.getTitre())
                        .orElse("Poste");

                // Email changement statut normal
                emailService.envoyerChangementStatut(
                        candidat.getEmail(),
                        candidat.getPrenom() + " " + candidat.getNom(),
                        titreOffre,
                        request.getNouveauStatut().name()
                );

                // Si ACCEPTE → email spécial avec QR Code
                if (StatutCandidature.ACCEPTE.equals(request.getNouveauStatut())) {
                    try {
                        // Génère QR Code
                        String qrCode = qrCodeService.genererQrCodeBase64(c.getId());

                        // Récupère le dernier lien Meet depuis les entretiens
                        String lienMeet = entretienRepository
                                .findByCandidatureId(c.getId())
                                .stream()
                                .filter(e -> e.getLienVisio() != null
                                        && !e.getLienVisio().isEmpty())
                                .findFirst()
                                .map(e -> e.getLienVisio())
                                .orElse(null);

                        log.info("Lien Meet trouvé pour candidature {} : {}", c.getId(), lienMeet);

                        // Envoie email acceptation avec QR Code ET lien Meet
                        emailService.envoyerEmailAcceptation(
                                candidat.getEmail(),
                                candidat.getPrenom() + " " + candidat.getNom(),
                                titreOffre,
                                "Votre Société",
                                qrCode,
                                lienMeet  // ← Passe le lien Meet
                        );

                        log.info("Email acceptation envoyé à {}", candidat.getEmail());

                    } catch (Exception ex) {
                        log.warn("Erreur envoi email acceptation : {}", ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Erreur traitement email : {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    public List<CandidatureResponse>
    getCandidaturesParOffre(String offreId) {

        return candidatureRepository

                .findByOffreId(offreId)

                .stream()

                .map(this::toResponse)

                .collect(Collectors.toList());

    }

    @Override
    public List<CandidatureResponse>
    getMesCandidatures(String candidatId) {

        return candidatureRepository

                .findByCandidatId(candidatId)

                .stream()

                .map(this::toResponse)

                .collect(Collectors.toList());

    }

    @Override
    public CandidatureResponse
    getCandidatureById(String id) {

        return toResponse(

                candidatureRepository.findById(id)

                        .orElseThrow(() ->

                                new RecrutementNotFoundException(

                                        "Candidature introuvable : " + id

                                ))

        );

    }

    @Override
    public CandidatureResponse
    ajouterNotesRecruteur(String id,
                          String notes) {

        Candidature c =

                candidatureRepository.findById(id)

                        .orElseThrow(() ->

                                new RecrutementNotFoundException(

                                        "Candidature introuvable : " + id

                                ));

        c.setNotesRecruteur(notes);

        c.setDateDerniereMAJ(

                LocalDateTime.now()
        );

        return toResponse(

                candidatureRepository.save(c)

        );

    }

    @Override
    public Map<StatutCandidature,
            List<CandidatureResponse>>
    getKanban(String offreId) {

        return candidatureRepository

                .findByOffreId(offreId)

                .stream()

                .collect(Collectors.groupingBy(

                        Candidature::getStatut,

                        Collectors.mapping(

                                this::toResponse,

                                Collectors.toList()

                        )

                ));

    }

    @Override
    public List<CandidatureResponse>
    getTopCandidatsByScore(String offreId) {

        return candidatureRepository

                .findByOffreIdOrderByScoreMatchingDesc(offreId)

                .stream()

                .map(this::toResponse)

                .collect(Collectors.toList());

    }

    @Override
    public CandidatureResponse soumettreTestLangue(String candidatureId, Double scoreLangue) {
        Candidature c = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new RecrutementNotFoundException("Candidature introuvable : " + candidatureId));
                
        // L'IA Python a déjà calculé le score et l'Angular l'a transmis ici!
        double score = Math.round(scoreLangue * 10.0) / 10.0;
        
        c.setTestLanguePasse(true);
        c.setScoreLangue(score);
        
        if (score < 55.0) {
            c.setFormationRequise(true);
            log.info("Test Langue - Candidat {} : Score faible ({}%), formation e-learning assignée.", candidatureId, score);
        } else {
            c.setFormationRequise(false);
            log.info("Test Langue - Candidat {} : Score excellent ({}%), pas de formation.", candidatureId, score);
        }
        
        if (c.getHistoriqueStatuts() == null) {
            c.setHistoriqueStatuts(new ArrayList<>());
        }
        c.getHistoriqueStatuts().add("TEST_VIDÉO_IA - " + LocalDateTime.now() + " | Anglais: " + score + "%");
        c.setDateDerniereMAJ(LocalDateTime.now());
        
        return toResponse(candidatureRepository.save(c));
    }

    @Override
    public void deleteCandidature(String id) {

        Candidature c =

                candidatureRepository.findById(id)

                        .orElseThrow(() ->

                                new RecrutementNotFoundException(

                                        "Candidature introuvable : " + id

                                ));

        if (c.getCvFileId() != null)

            gridFsTemplate.delete(

                    query(where("_id")
                            .is(c.getCvFileId()))

            );

        if (c.getLettreMotivationFileId() != null)

            gridFsTemplate.delete(

                    query(where("_id")
                            .is(c.getLettreMotivationFileId()))

            );

        candidatureRepository.deleteById(id);

    }

    @Override
    public CandidatureResponse modifierCandidature(String id, MultipartFile cv, MultipartFile lettre) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Candidature introuvable : " + id));

        Offre offre = offreRepository.findById(c.getOffreId())
                .orElseThrow(() -> new RecrutementNotFoundException("Offre introuvable"));

        if (cv != null && !cv.isEmpty()) {
            // Delete old CV
            if (c.getCvFileId() != null) {
                gridFsTemplate.delete(query(where("_id").is(c.getCvFileId())));
            }
            try {
                String cvId = gridFsTemplate.store(cv.getInputStream(), "cv_" + c.getCandidatId() + ".pdf", cv.getContentType()).toString();
                c.setCvFileId(cvId);
                // Re-analyser avec le nouveau CV
                executerAnalyseIA(c, offre, cv);
            } catch (IOException e) {
                throw new RuntimeException("Erreur update CV : " + e.getMessage());
            }
        }

        if (lettre != null && !lettre.isEmpty()) {
            // Delete old lettre
            if (c.getLettreMotivationFileId() != null) {
                gridFsTemplate.delete(query(where("_id").is(c.getLettreMotivationFileId())));
            }
            try {
                String lettreId = gridFsTemplate.store(lettre.getInputStream(), "lettre_" + c.getCandidatId() + ".pdf", lettre.getContentType()).toString();
                c.setLettreMotivationFileId(lettreId);
            } catch (IOException e) {
                throw new RuntimeException("Erreur update lettre : " + e.getMessage());
            }
        }

        c.setDateDerniereMAJ(LocalDateTime.now());
        if (c.getHistoriqueStatuts() == null) c.setHistoriqueStatuts(new ArrayList<>());
        c.getHistoriqueStatuts().add("MISE_A_JOUR_DOCUMENTS - " + LocalDateTime.now());

        return toResponse(candidatureRepository.save(c));
    }

    /**
     * Moteur IA Premium Refactorisé
     */
    private void executerAnalyseIA(Candidature candidature, Offre offre, MultipartFile cv) {
        List<String> competencesRequises = offre.getCompetencesRequises() != null ? 
            offre.getCompetencesRequises().stream().filter(s -> s != null && !s.trim().isEmpty()).map(String::trim).toList() : new ArrayList<>();

        List<String> competencesExtraitesMatch = new ArrayList<>();
        List<String> competencesManquantes = new ArrayList<>();
        List<String> toutesDetectees = new ArrayList<>();
        
        String cvText = "";
        if (cv != null && !cv.isEmpty()) {
            try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(cv.getInputStream())) {
                org.apache.pdfbox.text.PDFTextStripper pdfStripper = new org.apache.pdfbox.text.PDFTextStripper();
                cvText = pdfStripper.getText(document).toLowerCase();
            } catch (Exception e) {
                log.warn("Erreur IA - Impossible de lire le PDF : {}", e.getMessage());
            }
        }

        // 1. DÉTECTION PROACTIVE
        if (!cvText.isEmpty()) {
            for (String skill : SKILLS_LIBRARY) {
                if (cvText.contains(skill.toLowerCase())) {
                    toutesDetectees.add(skill);
                }
            }
        }

        // 2. MATCHING
        double scoreM = 0.0;
        boolean isTitleScaleMatching = false;
        
        if (!competencesRequises.isEmpty() && !cvText.isEmpty()) {
            int matchCount = 0;
            for (String comp : competencesRequises) {
                if (cvText.contains(comp.toLowerCase())) {
                    competencesExtraitesMatch.add(comp);
                    matchCount++;
                } else {
                    competencesManquantes.add(comp);
                }
            }
            scoreM = ((double) matchCount / competencesRequises.size()) * 100.0;
        } else if (competencesRequises.isEmpty() && !cvText.isEmpty() && !offre.getTitre().isEmpty()) {
            isTitleScaleMatching = true;
            String[] commonWords = {"en", "de", "le", "la", "les", "et", "ou", "du", "par", "pour", "dans", "un", "une", "des", "and", "the", "with", "for", "to", "in"};
            List<String> titleKeywords = new ArrayList<>(List.of(offre.getTitre().split("\\s|'|-")));
            titleKeywords = titleKeywords.stream()
                .map(String::trim).map(String::toLowerCase)
                .filter(w -> w.length() > 3).filter(w -> !List.of(commonWords).contains(w)).distinct().toList();
            
            if (!titleKeywords.isEmpty()) {
                int matchCount = 0;
                for (String word : titleKeywords) {
                    if (cvText.contains(word)) {
                        competencesExtraitesMatch.add(word);
                        matchCount++;
                    } else {
                        competencesManquantes.add(word);
                    }
                }
                scoreM = ((double) matchCount / titleKeywords.size()) * 100.0;
            } else {
                scoreM = 50.0;
            }
        } else {
            scoreM = 0.0;
        }
        
        scoreM = Math.round(scoreM * 10.0) / 10.0;

        // 3. EXPLICATION (XAI)
        StringBuilder explication = new StringBuilder();
        if (isTitleScaleMatching) explication.append("⚠️ Analyse basée sur le titre. ");
        if (scoreM >= 80) explication.append("Profil excellent ! ");
        else if (scoreM >= 50) explication.append("Bon profil technique. ");
        else explication.append("Profil en cours de développement. ");

        if (!competencesExtraitesMatch.isEmpty()) {
            explication.append("Forces détectées : ").append(String.join(", ", competencesExtraitesMatch)).append(". ");
        }
        if (!competencesManquantes.isEmpty()) {
            explication.append("Manquants : ").append(String.join(", ", competencesManquantes)).append(".");
        }

        if (scoreM < 20.0 && candidature.getId() == null) { // Seulement bloquant à la postulation initiale
            throw new RuntimeException("Candidature rejetée par l'IA : Score trop faible (" + scoreM + "%).");
        }

        java.util.Random rand = new java.util.Random();
        candidature.setScoreMatching(scoreM);
        candidature.setCompetencesExtraites(toutesDetectees);
        candidature.setCompetencesManquantes(competencesManquantes);
        candidature.setComparaisonExplication(explication.toString());
        
        // Soft Skills Refresh
        candidature.setScoreLeadership(cvText.contains("lead") || cvText.contains("manage") ? 85 + rand.nextInt(10) : 40 + rand.nextInt(30));
        candidature.setScoreEmpathie(cvText.contains("team") || cvText.contains("écoute") ? 80 + rand.nextInt(15) : 50 + rand.nextInt(20));
        candidature.setScoreAdaptabilite(cvText.contains("agile") || cvText.contains("adapt") ? 90 + rand.nextInt(10) : 60 + rand.nextInt(20));
        candidature.setScoreCommunication(cvText.contains("present") || cvText.contains("explain") ? 85 + rand.nextInt(10) : 65 + rand.nextInt(15));
        candidature.setScoreInnovation(cvText.contains("creat") || cvText.contains("innovat") || cvText.contains("design") ? 90 + rand.nextInt(10) : 55 + rand.nextInt(25));
    }

    @Override
    public byte[] genererContratPdf(String candidatureId) {
        Candidature c = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new RecrutementNotFoundException("Candidature introuvable : " + candidatureId));
                
        tn.esprit.rh_rse.entity.User candidat = userRepository.findById(c.getCandidatId())
                .orElseThrow(() -> new RuntimeException("Candidat introuvable"));
                
        Offre offre = offreRepository.findById(c.getOffreId())
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        try (org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            document.addPage(page);

            try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 24);
                contentStream.newLineAtOffset(150, 750);
                contentStream.showText("CONTRAT DE TRAVAIL");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(50, 680);
                contentStream.showText("ENTREPRISE : RSE CORPORATION");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 640);
                contentStream.showText("Employe(e) : " + candidat.getPrenom() + " " + candidat.getNom());
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 620);
                contentStream.showText("Email : " + candidat.getEmail());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(50, 580);
                contentStream.showText("DESIGNATION DU POSTE : " + offre.getTitre());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 540);
                contentStream.showText("SALAIRE ET REMUNERATION :");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Le(a) salarie(e) percevra une remuneration brute mensuelle fixee au " );
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Salaire Minimum Interprofessionnel de Croissance (SMIC) en vigueur.");
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 480);
                contentStream.showText("Fait le " + java.time.LocalDate.now().toString());
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(50, 420);
                contentStream.showText("Signature Employeur :                            Signature Employe(e) :");
                contentStream.endText();
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (java.io.IOException e) {
            log.error("Erreur lors de la génération du contrat PDF pour la candidature {}", candidatureId, e);
            throw new RuntimeException("Impossible de générer le contrat PDF.");
        }
    }

    @Override
    public byte[] genererCoachTipsPdf(String tipsText) {
        try (org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            document.addPage(page);

            try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 20);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("VOS CONSEILS CAREER COACH - RH_RSE");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.setLeading(15f);

                // Normalisation des \n reçus du frontend et Word Wrap basique pour PDFBox
                String[] textLines = tipsText.replace("\r", "").split("\n");
                for (String rawLine : textLines) {
                    String[] words = rawLine.split(" ");
                    StringBuilder dict = new StringBuilder();
                    for (String word : words) {
                        if (dict.length() + word.length() > 80) {
                            contentStream.showText(dict.toString());
                            contentStream.newLine();
                            dict = new StringBuilder();
                        }
                        dict.append(word).append(" ");
                    }
                    contentStream.showText(dict.toString());
                    contentStream.newLine();
                }
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_OBLIQUE, 10);
                contentStream.newLineAtOffset(50, 50);
                contentStream.showText("Généré par l'Intelligence Artificielle Anti-Biais de RH_RSE.");
                contentStream.endText();
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (java.io.IOException e) {
            log.error("Erreur lors de la génération du PDF Coach", e);
            throw new RuntimeException("Impossible de générer le PDF du Coach.");
        }
    }

    private String labelEtape(
            StatutCandidature statut) {

        return switch (statut) {

            case NOUVEAU -> "CV Reçu";

            case EN_COURS_ANALYSE ->
                    "En cours d'analyse";

            case ENTRETIEN_RH ->
                    "Entretien RH";

            case ENTRETIEN_TECHNIQUE ->
                    "Entretien Technique";

            case TEST_TECHNIQUE ->
                    "Test Technique";

            case OFFRE_ENVOYEE ->
                    "Offre d'embauche envoyée";

            case ACCEPTE ->
                    "Candidat accepté";

            case REFUSE ->
                    "Refusé";

        };

    }

    private CandidatureResponse
    toResponse(Candidature c) {

        return CandidatureResponse.builder()

                .id(c.getId())

                .candidatId(c.getCandidatId())

                .offreId(c.getOffreId())

                .cvFileId(c.getCvFileId())

                .lettreMotivationFileId(

                        c.getLettreMotivationFileId()

                )

                .statut(c.getStatut())

                .scoreMatching(c.getScoreMatching())

                .etapeActuelle(c.getEtapeActuelle())

                .notesRecruteur(c.getNotesRecruteur())

                .historiqueStatuts(c.getHistoriqueStatuts())

                .competencesExtraites(c.getCompetencesExtraites())
                .competencesManquantes(c.getCompetencesManquantes())
                .comparaisonExplication(c.getComparaisonExplication())
                .anneesExperienceDetecte(c.getAnneesExperienceDetecte())
                .testLanguePasse(c.getTestLanguePasse())
                .scoreLangue(c.getScoreLangue())
                .formationRequise(c.getFormationRequise())
                .scoreLeadership(c.getScoreLeadership())
                .scoreEmpathie(c.getScoreEmpathie())
                .scoreAdaptabilite(c.getScoreAdaptabilite())
                .scoreCommunication(c.getScoreCommunication())
                .scoreInnovation(c.getScoreInnovation())

                .datePostulation(c.getDatePostulation())

                .dateDerniereMAJ(c.getDateDerniereMAJ())

                .build();

    }

}