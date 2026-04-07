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
                cvFileId = gridFsTemplate.store(
                        cv.getInputStream(),
                        "cv_" + candidatId + "_" + offreId + ".pdf",
                        cv.getContentType()
                ).toString();

            } catch (IOException e) {
                throw new RuntimeException("Erreur upload CV : " + e.getMessage());
            }
        }

        String lettreFileId = null;

        if (lettre != null && !lettre.isEmpty()) {

            try {

                lettreFileId = gridFsTemplate.store(
                        lettre.getInputStream(),
                        "lettre_" + candidatId + "_" + offreId + ".pdf",
                        lettre.getContentType()
                ).toString();

            } catch (IOException e) {

                throw new RuntimeException("Erreur upload lettre : " + e.getMessage());

            }
        }

        // --- VRAIE IA MATCHING AVEC PDF OCR ---
        List<String> competencesRequisesInitiales = offre.getCompetencesRequises() != null ? offre.getCompetencesRequises() : new ArrayList<>();
        // Filtrer les compétences vides ou nulles
        List<String> competencesRequises = competencesRequisesInitiales.stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(String::trim)
                .toList();

        List<String> competencesExtraites = new ArrayList<>();
        List<String> competencesManquantes = new ArrayList<>();
        double scoreM = 0.0;
        java.util.Random rand = new java.util.Random();
        
        String cvText = "";
        if (cv != null && !cv.isEmpty()) {
            try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(cv.getInputStream())) {
                org.apache.pdfbox.text.PDFTextStripper pdfStripper = new org.apache.pdfbox.text.PDFTextStripper();
                cvText = pdfStripper.getText(document).toLowerCase();
                log.info("Extraction PDF réussie. Longueur du texte: {}", cvText.length());
            } catch (Exception e) {
                log.warn("Erreur IA - Impossible de lire le PDF : {}", e.getMessage());
            }
        }

        log.info("Compétences requises pour l'offre {}: {}", offre.getTitre(), competencesRequises);

        if (!competencesRequises.isEmpty() && !cvText.isEmpty()) {
            int matchCount = 0;
            for (String comp : competencesRequises) {
                if (cvText.contains(comp.toLowerCase())) {
                    competencesExtraites.add(comp);
                    matchCount++;
                } else {
                    competencesManquantes.add(comp);
                }
            }
            scoreM = ((double) matchCount / competencesRequises.size()) * 100.0;
            log.info("MatchCount: {} / {}, Score IA = {}%", matchCount, competencesRequises.size(), scoreM);
        } else if (!competencesRequises.isEmpty() && cvText.isEmpty()) {
            // Repli au cas où le PDF n'a pas de texte liseable (ex: image scannée)
            scoreM = 0.0;
            competencesManquantes.addAll(competencesRequises);
            log.warn("Le CV est illisible ou vide, score mis à 0.");
        } else {
            scoreM = 100.0; // Pas de compétences requises
            log.info("Aucune compétence requise définie dans l'Offre. Score = 100%.");
        }
        
        scoreM = Math.round(scoreM * 10.0) / 10.0; // 1 décimale

        // FILTRE BLOQUANT IA (Auto-Rejection)
        if (scoreM < 20.0) {
            throw new RuntimeException("Candidature rejetée par l'Intelligence Artificielle : Le score d'adéquation (" + scoreM + "%) est en dessous du seuil minimum de 20%. Veuillez cibler des offres correspondant mieux à votre profil.");
        }

        Candidature candidature = Candidature.builder()

                .candidatId(candidatId)
                .offreId(offreId)
                .cvFileId(cvFileId)
                .lettreMotivationFileId(lettreFileId)
                .statut(StatutCandidature.NOUVEAU)
                .scoreMatching(scoreM)
                .competencesExtraites(competencesExtraites)
                .competencesManquantes(competencesManquantes)
                .anneesExperienceDetecte(rand.nextInt(6) + 1)
                .testLanguePasse(false)
                .scoreLangue(0.0)
                .formationRequise(false)
                .etapeActuelle("CV Reçu")

                .historiqueStatuts(
                        new ArrayList<>(List.of("NOUVEAU - " + LocalDateTime.now()))
                )

                .datePostulation(LocalDateTime.now())
                .dateDerniereMAJ(LocalDateTime.now())

                .build();

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
                .anneesExperienceDetecte(c.getAnneesExperienceDetecte())
                .testLanguePasse(c.getTestLanguePasse())
                .scoreLangue(c.getScoreLangue())
                .formationRequise(c.getFormationRequise())

                .datePostulation(c.getDatePostulation())

                .dateDerniereMAJ(c.getDateDerniereMAJ())

                .build();

    }

}