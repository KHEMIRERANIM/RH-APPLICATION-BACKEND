package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.Formation.*;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.entity.Formation.FormationProposition;
import tn.esprit.rh_rse.entity.Formation.ParticipantInscription;
import tn.esprit.rh_rse.entity.Formation.VoteProposition;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.Formation.FormationPropositionRepository;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.Formation.*;
import tn.esprit.rh_rse.service.Formation.StripeHttpService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
public class FormationController {

    private final FormationService formationService;
    private final InscriptionService inscriptionService;
    private final VotePropositionService votePropositionService;
    private final QrCodeFormationService qrCodeFormationService;
    private final FormationCompletionService formationCompletionService;
    private final PointFormationService pointService;
    private final UserRepository userRepository;
    private final FormationRepository formationRepository;
    private final FormationPropositionRepository propositionRepository;

    @GetMapping
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<List<FormationDTO>> getAllFormations() {
        return ResponseEntity.ok(formationService.getAllFormations());
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<FormationDTO>> getFormationsDisponibles() {
        return ResponseEntity.ok(formationService.getFormationsDisponibles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormationDTO> getFormationById(@PathVariable String id) {
        return ResponseEntity.ok(formationService.getFormationById(id));
    }

    @PostMapping
    public ResponseEntity<FormationDTO> createFormation(@RequestBody FormationDTO formationDTO) {
        return new ResponseEntity<>(formationService.createFormation(formationDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormationDTO> updateFormation(@PathVariable String id,
                                                        @RequestBody FormationDTO formationDTO) {
        return ResponseEntity.ok(formationService.updateFormation(id, formationDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFormation(@PathVariable String id) {
        formationService.deleteFormation(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Void> toggleActive(@PathVariable String id) {
        formationService.toggleActive(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<FormationDTO>> getFormationsByType(@PathVariable String type) {
        return ResponseEntity.ok(formationService.getFormationsByType(type));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FormationDTO>> searchFormations(@RequestParam String keyword) {
        return ResponseEntity.ok(formationService.searchFormations(keyword));
    }

    @PostMapping("/{formationId}/inscrire/{employeId}")
    public ResponseEntity<?> inscrireEmploye(
            @PathVariable String formationId,
            @PathVariable String employeId) {
        try {
            log.info("📝 Requête d'inscription - Formation: {}, Employé: {}", formationId, employeId);

            ParticipantInscription inscription = inscriptionService.inscrireEmployeParticipant(formationId, employeId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Inscription réussie",
                    "inscription", inscription
            ));

        } catch (RuntimeException e) {
            log.error("❌ Erreur inscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    // Dans FormationController.java
    // Méthode existante - à modifier
    @DeleteMapping("/inscriptions/{inscriptionId}")
    public ResponseEntity<Void> annulerInscription(
            @PathVariable String inscriptionId,
            @RequestParam String motif) {
        inscriptionService.annulerInscription(inscriptionId, motif);
        return ResponseEntity.ok().build();
    }

    // ✅ AJOUTER CETTE NOUVELLE MÉTHODE avec le corps JSON
    @PostMapping("/inscriptions/{inscriptionId}/annuler")
    public ResponseEntity<?> annulerInscriptionAvecMotif(
            @PathVariable String inscriptionId,
            @RequestBody Map<String, String> payload) {
        try {
            String motif = payload.get("raison");
            String typeMotif = payload.get("typeMotif");

            log.info("📝 Annulation inscription: {}, Motif: {}, Type: {}", inscriptionId, motif, typeMotif);

            // Appeler le service avec l'employeId récupéré depuis l'inscription
            Map<String, Object> result = inscriptionService.annulerInscription(inscriptionId, motif, typeMotif);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("❌ Erreur annulation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/employe/{employeId}/inscriptions")
    public ResponseEntity<List<InscriptionFormationDTO>> getInscriptionsByEmploye(
            @PathVariable String employeId) {
        return ResponseEntity.ok(inscriptionService.getInscriptionsByEmploye(employeId));
    }

    @GetMapping("/{formationId}/inscriptions")
    public ResponseEntity<List<InscriptionFormationDTO>> getInscriptionsByFormation(
            @PathVariable String formationId) {
        return ResponseEntity.ok(inscriptionService.getInscriptionsByFormation(formationId));
    }

    @PatchMapping("/inscriptions/{inscriptionId}/presence")
    public ResponseEntity<Void> confirmerPresence(@PathVariable String inscriptionId) {
        inscriptionService.confirmerPresence(inscriptionId);
        return ResponseEntity.ok().build();
    }

    // ✅ ENDPOINT QR CODE AVEC INFOS FORMATION
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<Map<String, Object>> getFormationQRCode(@PathVariable String id) {
        try {
            FormationDTO formation = formationService.getFormationById(id);

            // Créer les données du QR Code avec les informations de la formation
            Map<String, Object> qrData = new HashMap<>();
            qrData.put("formationId", formation.getId());
            qrData.put("formationTitre", formation.getTitre());
            qrData.put("formateur", formation.getFormateur());
            qrData.put("dateDebut", formation.getDateDebut());
            qrData.put("dateFin", formation.getDateFin());
            qrData.put("lieu", formation.getLieu());
            qrData.put("type", formation.getType());

            String qrCodeBase64 = qrCodeFormationService.genererQRCodeFormation(qrData);

            Map<String, Object> response = new HashMap<>();
            response.put("qrCode", qrCodeBase64);
            response.put("formationId", formation.getId());
            response.put("formationTitre", formation.getTitre());
            response.put("formateur", formation.getFormateur());
            response.put("dateDebut", formation.getDateDebut());
            response.put("dateFin", formation.getDateFin());
            response.put("lieu", formation.getLieu());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur génération QR Code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/update-completed")
    public ResponseEntity<Map<String, String>> updateCompletedFormations() {
        formationCompletionService.updateCompletedFormations();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Mise à jour des formations terminées effectuée");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/employe/{employeId}/update-to-termine")
    public ResponseEntity<Void> updateToTermine(@PathVariable String employeId) {
        inscriptionService.updateToTermine(employeId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/inscriptions/{inscriptionId}/termine")
    public ResponseEntity<Void> setTermine(@PathVariable String inscriptionId) {
        inscriptionService.setTermine(inscriptionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/employe/{employeId}/inscriptions/debug")
    public ResponseEntity<Void> debug(@PathVariable String employeId) {
        inscriptionService.debugInscriptions(employeId);
        return ResponseEntity.ok().build();
    }
// Dans FormationController.java - Ajoutez cette méthode

    @GetMapping("/terminees")
    public ResponseEntity<List<FormationDTO>> getFormationsTerminees() {
        log.info("📋 GET /api/formations/terminees");
        return ResponseEntity.ok(formationService.getFormationsTerminees());
    }
    // ==================== ENDPOINTS POINTS ====================

    @GetMapping("/points/{employeId}")
    public ResponseEntity<Map<String, Object>> getPoints(@PathVariable String employeId) {
        Map<String, Object> response = new HashMap<>();
        response.put("employeId", employeId);
        response.put("solde", pointService.getSoldePoints(employeId));
        response.put("historique", pointService.getHistoriquePoints(employeId));
        return ResponseEntity.ok(response);
    }


    @PostMapping("/points/{employeId}/recharger")
    public ResponseEntity<Void> rechargerPoints(@PathVariable String employeId) {
        User employe = userRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        pointService.rechargerPointsEmploye(employeId, employe.getNom() + " " + employe.getPrenom());
        return ResponseEntity.ok().build();
    }

// ==================== ENDPOINTS STRIPE ====================


    @GetMapping("/points/debug/{employeId}")
    public ResponseEntity<?> debugPoints(@PathVariable String employeId) {
        Map<String, Object> debug = pointService.debugPoints(employeId);
        return ResponseEntity.ok(debug);
    }

    private final StripeHttpService stripeHttpService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    // ==================== ENDPOINTS STRIPE ====================

    @PostMapping("/payments/create-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentRequestDTO req) {
        try {
            log.info("📝 Création PaymentIntent - userId: {}, points: {}, amount: {}€",
                    req.getUserId(), req.getPoints(), req.getAmount());

            String clientSecret = stripeHttpService.createPaymentIntent(
                    req.getUserId(),
                    req.getPoints(),
                    req.getAmount()
            );

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", clientSecret);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur création PaymentIntent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/refresh-inscriptions")
    public ResponseEntity<?> refreshInscriptions(@PathVariable String id) {
        formationService.mettreAJourStatutsInscriptions();
        return ResponseEntity.ok().body(Map.of("message", "Statuts mis à jour"));
    }
    // Ajoutez cette méthode dans votre FormationController.java

    // Dans FormationController.java
    @PostMapping("/{formationId}/confirmer-presence/{employeId}")
    public ResponseEntity<?> confirmerPresence(
            @PathVariable String formationId,
            @PathVariable String employeId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String commentaire = body != null ? body.get("commentaire") : null;
            ParticipantInscription inscription = inscriptionService.validerPresenceParticipant(
                    formationId, employeId, commentaire);

            // Récupérer la formation mise à jour
            FormationDTO formation = formationService.getFormationById(formationId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Présence confirmée et places mises à jour",
                    "inscription", inscription,
                    "placesRestantes", formation.getPlacesDisponibles()
            ));
        } catch (RuntimeException e) {
            log.error("❌ Erreur confirmation présence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/inscriptions/employe/{employeId}")
    public ResponseEntity<?> getInscriptionsByEmployeId(@PathVariable String employeId) {
        log.info("📋 Récupération des inscriptions pour l'employé: {}", employeId);

        try {
            // Appeler le service existant
            List<InscriptionFormationDTO> inscriptions = inscriptionService.getInscriptionsByEmploye(employeId);

            // Ajouter les détails de la formation pour chaque inscription
            for (InscriptionFormationDTO inscription : inscriptions) {
                Formation formation = formationRepository.findById(inscription.getFormationId()).orElse(null);
                if (formation != null) {
                    inscription.setFormationTitre(formation.getTitre());
                    inscription.setDateDebut(formation.getDateDebut());
                    inscription.setDateFin(formation.getDateFin());
                    inscription.setFormateur(formation.getFormateur());
                    inscription.setLieu(formation.getLieu());
                    inscription.setLienVisio(formation.getLienVisio());
                    inscription.setLienGoogleMaps(formation.getLienGoogleMaps());
                }
            }

            log.info("✅ {} inscriptions trouvées", inscriptions.size());
            return ResponseEntity.ok(inscriptions);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des inscriptions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Dans FormationController.java - AJOUTER CES ENDPOINTS

// ==================== ENDPOINTS RECOMMANDATION INTELLIGENTE ====================

    @Autowired
    private TechWatchService techWatchService;

    @Autowired
    private RAGRecommendationService ragRecommendationService;

    @GetMapping("/recommendations/technologies")
    public ResponseEntity<List<RecommendationDTO>> getTechnologieRecommendations() {
        log.info("🔍 Génération des recommandations technologiques...");
        List<FormationDTO> formations = formationService.getAllFormations();
        List<RecommendationDTO> recommandations = techWatchService.detectTechnologieGaps(formations);
        return ResponseEntity.ok(recommandations);
    }

    @GetMapping("/recommendations/employe/{employeId}")
    public ResponseEntity<RecommandationPersonnaliseeDTO> getPersonalizedRecommendations(
            @PathVariable String employeId) {
        log.info("🎯 Recommandations personnalisées pour employé: {}", employeId);

        // Récupérer l'historique des inscriptions de l'employé
        List<InscriptionFormationDTO> historique = inscriptionService.getInscriptionsByEmploye(employeId);

        // Convertir en ParticipantInscription (ou adapter)
        List<ParticipantInscription> inscriptions = historique.stream()
                .map(h -> {
                    ParticipantInscription ins = new ParticipantInscription();
                    ins.setFormationId(h.getFormationId());
                    ins.setStatut(h.getStatut());
                    return ins;
                })
                .collect(Collectors.toList());

        RecommandationPersonnaliseeDTO result = ragRecommendationService.recommanderPourEmploye(employeId, inscriptions);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/recommendations/index")
    public ResponseEntity<Void> indexFormations() {
        ragRecommendationService.indexerFormations();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/trending")
    public ResponseEntity<List<String>> getTrendingTechnologies() {
        return ResponseEntity.ok(techWatchService.getGitHubTrends());
    }


    @Autowired
    private FormationPropositionService propositionService;

    /**
     * Générer des propositions automatiques (IA)
     */
    @PostMapping("/propositions/generer")
    public ResponseEntity<List<FormationPropositionDTO>> genererPropositions() {
        log.info("🤖 Génération automatique des propositions de formation");
        List<FormationProposition> propositions = propositionService.genererPropositionsIA();
        return ResponseEntity.ok(propositions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Récupérer toutes les propositions en attente
     */
    @GetMapping("/propositions/en-attente")
    public ResponseEntity<List<FormationPropositionDTO>> getPropositionsEnAttente() {
        return ResponseEntity.ok(propositionService.getPropositionsEnAttente()
                .stream().map(this::convertToDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Valider une proposition (Admin RH)
     */
    @PostMapping("/propositions/{propositionId}/valider")
    public ResponseEntity<FormationPropositionDTO> validerProposition(
            @PathVariable String propositionId,
            @RequestParam String adminId,
            @RequestBody Map<String, String> body) {
        String commentaire = body.get("commentaire");
        FormationProposition proposition = propositionService.validerProposition(propositionId, adminId, commentaire);
        return ResponseEntity.ok(convertToDTO(proposition));
    }

    /**
     * Rejeter une proposition (Admin RH)
     */
    @PostMapping("/propositions/{propositionId}/rejeter")
    public ResponseEntity<FormationPropositionDTO> rejeterProposition(
            @PathVariable String propositionId,
            @RequestParam String adminId,
            @RequestBody Map<String, String> body) {
        String raison = body.get("raison");
        FormationProposition proposition = propositionService.rejeterProposition(propositionId, adminId, raison);
        return ResponseEntity.ok(convertToDTO(proposition));
    }

    /**
     * Publier une formation (Admin RH)
     */
    @PostMapping("/propositions/{propositionId}/publier")
    public ResponseEntity<FormationDTO> publierFormation(@PathVariable String propositionId) {
        Formation formation = propositionService.publierFormation(propositionId);
        return ResponseEntity.ok(convertToFormationDTO(formation));
    }

    /**
     * Employer manifeste son intérêt pour une proposition
     */
    @PostMapping("/propositions/{propositionId}/interet")
    public ResponseEntity<?> manifesterInteret(
            @PathVariable String propositionId,
            @RequestBody Map<String, String> body) {
        String employeId = body.get("employeId");
        String employeNom = body.get("employeNom");
        String employeEmail = body.get("employeEmail");

        FormationProposition proposition = propositionService.ajouterInteret(propositionId, employeId, employeNom, employeEmail);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Votre intérêt a été enregistré !");
        response.put("totalInterets", proposition.getEmployesInteresses().size());

        return ResponseEntity.ok(response);
    }

    /**
     * Statistiques des propositions
     */
    @GetMapping("/propositions/stats")
    public ResponseEntity<Map<String, Object>> getPropositionStats() {
        return ResponseEntity.ok(propositionService.getStatistiques());
    }


    /**
     * Convertit une entité Formation en FormationDTO
     */
    private FormationDTO convertToFormationDTO(Formation formation) {
        if (formation == null) return null;

        FormationDTO dto = new FormationDTO();
        dto.setId(formation.getId());
        dto.setTitre(formation.getTitre());
        dto.setDescription(formation.getDescription());
        dto.setObjectifs(formation.getObjectifs());
        dto.setPreRequis(formation.getPreRequis());
        dto.setType(formation.getType());
        dto.setDureeHeures(formation.getDureeHeures());
        dto.setNombrePlaces(formation.getNombrePlaces());
        dto.setPlacesDisponibles(formation.getPlacesDisponibles());
        dto.setNiveau(formation.getNiveau());
        dto.setFormateur(formation.getFormateur());
        dto.setFormateurBio(formation.getFormateurBio());
        dto.setLieu(formation.getLieu());
        dto.setLienVisio(formation.getLienVisio());
        dto.setLienGoogleMaps(formation.getLienGoogleMaps());
        dto.setDateDebut(formation.getDateDebut());
        dto.setDateFin(formation.getDateFin());
        dto.setDateLimiteInscription(formation.getDateLimiteInscription());
        dto.setImageUrl(formation.getImageUrl());
        dto.setActive(formation.isActive());
        dto.setPrerequisFormationId(formation.getPrerequisFormationId());
        dto.setPrerequisFormationTitre(formation.getPrerequisFormationTitre());
        dto.setNoteMoyenne(formation.getNoteMoyenne());


        return dto;
    }

    /**
     * Convertit une entité FormationProposition en FormationPropositionDTO
     */
    private FormationPropositionDTO convertToDTO(FormationProposition p) {
        if (p == null) return null;

        FormationPropositionDTO dto = new FormationPropositionDTO();
        dto.setId(p.getId());
        dto.setTechnologie(p.getTechnologie());
        dto.setTitre(p.getTitre());
        dto.setDescription(p.getDescription());
        dto.setObjectifs(p.getObjectifs());
        dto.setDureeHeures(p.getDureeHeures());
        dto.setNiveau(p.getNiveau());
        dto.setPublicCible(p.getPublicCible());
        dto.setPreRequis(p.getPreRequis());
        dto.setType(p.getType());
        dto.setSource(p.getSource());
        dto.setScoreIA(p.getScoreIA());
        dto.setStatut(p.getStatut());
        dto.setCommentaireValidation(p.getCommentaireValidation());
        dto.setDateProposition(p.getDateProposition());
        dto.setDateValidation(p.getDateValidation());
        dto.setValidePar(p.getValidePar());
        dto.setFormationCreeeId(p.getFormationCreeeId());

        // Nombre d'intérêts
        if (p.getEmployesInteresses() != null) {
            dto.setNombreInteresses(p.getEmployesInteresses().size());

            // Convertir la liste des employés intéressés
            List<FormationPropositionDTO.InteretEmployeDTO> interetsDto = p.getEmployesInteresses().stream()
                    .map(interet -> {
                        FormationPropositionDTO.InteretEmployeDTO interetDto = new FormationPropositionDTO.InteretEmployeDTO();
                        interetDto.setEmployeId(interet.getEmployeId());
                        interetDto.setEmployeNom(interet.getEmployeNom());
                        interetDto.setEmployeEmail(interet.getEmployeEmail());
                        interetDto.setDateInteret(interet.getDateInteret());
                        return interetDto;
                    })
                    .collect(Collectors.toList());
            dto.setEmployesInteresses(interetsDto);
        } else {
            dto.setNombreInteresses(0);
            dto.setEmployesInteresses(List.of());
        }

        return dto;
    }


    // Dans FormationController.java - CORRECTION


    // ==================== ENDPOINTS PROPOSITIONS ====================

    /**
     * Récupérer toutes les propositions validées (pour les votes)
     */
    @GetMapping("/propositions/validees")
    public ResponseEntity<List<FormationPropositionDTO>> getPropositionsValidees() {
        log.info("📋 Récupération des propositions validées");
        List<FormationProposition> propositions = propositionService.getPropositionsValidees();
        return ResponseEntity.ok(propositions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Créer une proposition manuellement (à partir d'une recommandation IA)
     */
    @PostMapping("/propositions")
    public ResponseEntity<FormationPropositionDTO> createProposition(@RequestBody Map<String, Object> propositionData) {
        try {
            log.info("📝 Création d'une nouvelle proposition: {}", propositionData.get("titre"));

            FormationProposition proposition = new FormationProposition();
            proposition.setTechnologie((String) propositionData.get("technologie"));
            proposition.setTitre((String) propositionData.get("titre"));
            proposition.setDescription((String) propositionData.get("description"));
            proposition.setObjectifs("Maîtriser les fondamentaux de " + propositionData.get("technologie"));
            proposition.setDureeHeures(propositionData.get("dureeHeures") != null ?
                    (Integer) propositionData.get("dureeHeures") : 14);
            proposition.setNiveau(propositionData.get("niveau") != null ?
                    (String) propositionData.get("niveau") : "INTERMEDIAIRE");
            proposition.setType(propositionData.get("type") != null ?
                    (String) propositionData.get("type") : "TECHNIQUE");
            proposition.setSource(propositionData.get("source") != null ?
                    (String) propositionData.get("source") : "IA_RECOMMENDATION");
            proposition.setScoreIA(propositionData.get("scoreIA") != null ?
                    ((Number) propositionData.get("scoreIA")).doubleValue() : 85.0);
            proposition.setStatut("EN_ATTENTE_VALIDATION");
            proposition.setDateProposition(LocalDateTime.now());
            proposition.setEmployesInteresses(new ArrayList<>());
            proposition.setVotes(new ArrayList<>());

            FormationProposition saved = propositionRepository.save(proposition);
            log.info("✅ Proposition créée avec succès: {}", saved.getId());

            return ResponseEntity.ok(convertToDTO(saved));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Ouvrir le vote pour une proposition
     */
    @PostMapping("/propositions/{propositionId}/ouvrir-vote")
    public ResponseEntity<FormationPropositionDTO> ouvrirVote(
            @PathVariable String propositionId,
            @RequestBody Map<String, Integer> body) {
        try {
            int dureeJours = body.getOrDefault("dureeJours", 7);
            int seuilMinimum = body.getOrDefault("seuilMinimum", 5);

            FormationProposition proposition = propositionService.ouvrirVote(propositionId, dureeJours, seuilMinimum);
            return ResponseEntity.ok(convertToDTO(proposition));
        } catch (Exception e) {
            log.error("Erreur ouverture vote: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clôturer le vote pour une proposition
     */
    @PostMapping("/propositions/{propositionId}/cloturer-vote")
    public ResponseEntity<FormationPropositionDTO> cloturerVote(@PathVariable String propositionId) {
        try {
            FormationProposition proposition = votePropositionService.cloturerVote(propositionId);
            return ResponseEntity.ok(convertToDTO(proposition));
        } catch (Exception e) {
            log.error("Erreur clôture vote: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer les statistiques de vote
     */
    @GetMapping("/propositions/{propositionId}/stats-vote")
    public ResponseEntity<Map<String, Object>> getStatsVote(@PathVariable String propositionId) {
        return ResponseEntity.ok(votePropositionService.getStatsVote(propositionId));
    }

    /**
     * Vérifier si un employé a voté
     */
    @GetMapping("/propositions/{propositionId}/a-vote/{employeId}")
    public ResponseEntity<Map<String, Object>> hasEmployeVoted(
            @PathVariable String propositionId,
            @PathVariable String employeId) {
        boolean aVote = votePropositionService.hasEmployeVoted(propositionId, employeId);
        Map<String, Object> response = new HashMap<>();
        response.put("aVote", aVote);

        if (aVote) {
            Optional<VoteProposition> voteOpt = votePropositionService.getEmployeVote(propositionId, employeId);
            voteOpt.ifPresent(vote -> {
                Map<String, Object> voteData = new HashMap<>();
                voteData.put("vote", vote.getVote());
                voteData.put("commentaire", vote.getCommentaire());
                voteData.put("dateVote", vote.getDateVote());
                response.put("vote", voteData);
            });
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Employer vote pour une proposition
     */
    @PostMapping("/propositions/{propositionId}/voter")
    public ResponseEntity<?> voter(
            @PathVariable String propositionId,
            @RequestBody Map<String, String> body) {
        try {
            String employeId = body.get("employeId");
            String employeNom = body.get("employeNom");
            String employeEmail = body.get("employeEmail");
            String vote = body.get("vote");
            String commentaire = body.get("commentaire");

            FormationProposition proposition = votePropositionService.voter(propositionId, employeId,
                    employeNom, employeEmail, vote, commentaire);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Votre vote a été enregistré");
            response.put("pourcentagePour", proposition.getPourcentagePour());
            response.put("totalVotes", proposition.getTotalVotes());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur vote: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    /**
     * Récupérer les propositions ouvertes au vote (pour les employés)
     * Inclut les propositions avec statutVote = 'OUVERT'
     */
    @GetMapping("/propositions/vote/ouvertes")
    public ResponseEntity<List<FormationPropositionDTO>> getPropositionsOuvertesVote() {
        log.info("📋 Récupération des propositions ouvertes au vote");
        // Récupérer toutes les propositions avec statutVote = 'OUVERT'
        List<FormationProposition> propositions = propositionRepository.findByStatutVote("OUVERT");
        return ResponseEntity.ok(propositions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
    }

}
