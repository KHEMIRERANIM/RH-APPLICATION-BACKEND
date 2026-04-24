package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.Formation.FormationDTO;
import tn.esprit.rh_rse.dto.Formation.RecommendationDTO;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.entity.Formation.FormationProposition;
import tn.esprit.rh_rse.entity.Formation.VoteProposition;
import tn.esprit.rh_rse.repository.Formation.FormationPropositionRepository;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FormationPropositionService {

    private final FormationPropositionRepository propositionRepository;
    private final FormationRepository formationRepository;
    private final TechWatchService techWatchService;
    private final FormationService formationService;

    /**
     * Génère des propositions de formations via l'IA
     */
    public List<FormationProposition> genererPropositionsIA() {
        List<FormationProposition> propositions = new ArrayList<>();

        // ✅ CORRECTION : Récupérer les formations existantes
        List<FormationDTO> formationsExistantes = formationService.getAllFormations();

        // ✅ CORRECTION : Appeler correctement techWatchService
        List<RecommendationDTO> recommandations = techWatchService.detectTechnologieGaps(formationsExistantes);

        if (recommandations == null || recommandations.isEmpty()) {
            log.warn("Aucune recommandation générée par TechWatchService");
            return propositions;
        }

        for (RecommendationDTO rec : recommandations) {
            // Vérifier si une proposition existe déjà pour cette technologie
            boolean existeDeja = propositionRepository.existsByTechnologieAndStatutIn(
                    rec.getTechnologie(),
                    Arrays.asList("EN_ATTENTE_VALIDATION", "VALIDEE", "PROGRAMMEE")
            );

            if (!existeDeja && "HAUTE".equals(rec.getPriorite())) {
                FormationProposition proposition = new FormationProposition();
                proposition.setTechnologie(rec.getTechnologie());
                proposition.setTitre("Formation " + rec.getTechnologie());
                proposition.setDescription(rec.getSuggestionsFormations() != null && !rec.getSuggestionsFormations().isEmpty()
                        ? rec.getSuggestionsFormations().get(0)
                        : "Formation complète sur " + rec.getTechnologie());
                proposition.setObjectifs("Maîtriser les fondamentaux de " + rec.getTechnologie());
                proposition.setSource("IA_RECOMMENDATION");
                proposition.setScoreIA(rec.getScore());
                proposition.setStatut("EN_ATTENTE_VALIDATION");
                proposition.setDureeHeures(14);
                proposition.setNiveau("INTERMEDIAIRE");
                proposition.setType(detecterType(rec.getTechnologie()));
                proposition.setDateProposition(LocalDateTime.now());
                proposition.setEmployesInteresses(new ArrayList<>());

                propositions.add(propositionRepository.save(proposition));
                log.info("📝 Proposition générée: {}", proposition.getTitre());
            }
        }

        log.info("✅ {} propositions générées par l'IA", propositions.size());
        return propositions;
    }

    /**
     * Récupère toutes les propositions en attente
     */
    public List<FormationProposition> getPropositionsEnAttente() {
        return propositionRepository.findByStatut("EN_ATTENTE_VALIDATION");
    }

    /**
     * Récupère toutes les propositions validées
     */
    public List<FormationProposition> getPropositionsValidees() {
        return propositionRepository.findByStatut("VALIDEE");
    }

    /**
     * Valide une proposition (par l'admin RH)
     */
    public FormationProposition validerProposition(String propositionId, String adminId, String commentaire) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        proposition.setStatut("VALIDEE");
        proposition.setDateValidation(LocalDateTime.now());
        proposition.setValidePar(adminId);
        proposition.setCommentaireValidation(commentaire);

        log.info("✅ Proposition validée: {} par {}", proposition.getTitre(), adminId);
        return propositionRepository.save(proposition);
    }

    /**
     * Rejette une proposition
     */
    public FormationProposition rejeterProposition(String propositionId, String adminId, String raison) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        proposition.setStatut("REJETEE");
        proposition.setDateValidation(LocalDateTime.now());
        proposition.setValidePar(adminId);
        proposition.setCommentaireValidation(raison);

        log.info("❌ Proposition rejetée: {} - Raison: {}", proposition.getTitre(), raison);
        return propositionRepository.save(proposition);
    }

    /**
     * Publie une formation validée (la rend disponible aux employés)
     */
    public Formation publierFormation(String propositionId) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        if (!"VALIDEE".equals(proposition.getStatut())) {
            throw new RuntimeException("La proposition doit être validée avant publication");
        }

        // Créer la formation réelle
        Formation formation = new Formation();
        formation.setTitre(proposition.getTitre());
        formation.setDescription(proposition.getDescription());
        formation.setObjectifs(proposition.getObjectifs());
        formation.setType(proposition.getType());
        formation.setDureeHeures(proposition.getDureeHeures());
        formation.setNiveau(proposition.getNiveau());
        formation.setActive(true);
        formation.setNombrePlaces(15);
        formation.setPlacesDisponibles(15);
        formation.setDateDebut(LocalDateTime.now().plusWeeks(2));
        formation.setDateFin(LocalDateTime.now().plusWeeks(3));
        formation.setCreatedAt(LocalDateTime.now());

        Formation savedFormation = formationRepository.save(formation);

        // Mettre à jour la proposition
        proposition.setStatut("PROGRAMMEE");
        proposition.setFormationCreeeId(savedFormation.getId());
        propositionRepository.save(proposition);

        log.info("📢 Formation '{}' publiée avec succès - ID: {}", proposition.getTitre(), savedFormation.getId());

        return savedFormation;
    }

    /**
     * Un employé marque son intérêt pour une proposition
     */
    public FormationProposition ajouterInteret(String propositionId, String employeId, String employeNom, String employeEmail) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        // Vérifier si l'employé a déjà manifesté son intérêt
        boolean dejaInteresse = proposition.getEmployesInteresses() != null &&
                proposition.getEmployesInteresses().stream()
                        .anyMatch(e -> e.getEmployeId().equals(employeId));

        if (!dejaInteresse) {
            FormationProposition.InteretEmploye interet = new FormationProposition.InteretEmploye();
            interet.setEmployeId(employeId);
            interet.setEmployeNom(employeNom);
            interet.setEmployeEmail(employeEmail);
            interet.setDateInteret(LocalDateTime.now());

            if (proposition.getEmployesInteresses() == null) {
                proposition.setEmployesInteresses(new ArrayList<>());
            }
            proposition.getEmployesInteresses().add(interet);
            proposition = propositionRepository.save(proposition);

            log.info("👍 Employé {} intéressé par la formation '{}' (Total: {})",
                    employeNom, proposition.getTitre(), proposition.getEmployesInteresses().size());

            // Vérifier si seuil atteint
            verifierSeuilProgrammation(proposition);
        }

        return proposition;
    }

    /**
     * Vérifie si assez d'employés sont intéressés
     */
    private void verifierSeuilProgrammation(FormationProposition proposition) {
        int SEUIL_MINIMUM = 10;

        if (proposition.getEmployesInteresses() != null &&
                proposition.getEmployesInteresses().size() >= SEUIL_MINIMUM &&
                "VALIDEE".equals(proposition.getStatut())) {
            log.info("🎯 Seuil atteint pour la formation '{}' - {} employés intéressés",
                    proposition.getTitre(), proposition.getEmployesInteresses().size());
        }
    }

    /**
     * Statistiques des propositions
     */
    public Map<String, Object> getStatistiques() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("en_attente", propositionRepository.countByStatut("EN_ATTENTE_VALIDATION"));
        stats.put("validees", propositionRepository.countByStatut("VALIDEE"));
        stats.put("rejetees", propositionRepository.countByStatut("REJETEE"));
        stats.put("programmees", propositionRepository.countByStatut("PROGRAMMEE"));

        // Calculer le total des intérêts
        long totalInterets = 0;
        List<FormationProposition> toutes = propositionRepository.findAll();
        for (FormationProposition p : toutes) {
            if (p.getEmployesInteresses() != null) {
                totalInterets += p.getEmployesInteresses().size();
            }
        }
        stats.put("total_interets", totalInterets);

        return stats;
    }

    /**
     * Détecte le type de formation basé sur la technologie
     */
    private String detecterType(String technologie) {
        String techLower = technologie.toLowerCase();
        if (techLower.contains("angular") || techLower.contains("react") ||
                techLower.contains("vue") || techLower.contains("java") ||
                techLower.contains("python") || techLower.contains("docker") ||
                techLower.contains("kubernetes") || techLower.contains("aws")) {
            return "TECHNIQUE";
        }
        if (techLower.contains("leadership") || techLower.contains("management") ||
                techLower.contains("team") || techLower.contains("agile")) {
            return "MANAGERIAL";
        }
        if (techLower.contains("rse") || techLower.contains("environnement") ||
                techLower.contains("developpement durable")) {
            return "RSE";
        }
        if (techLower.contains("communication") || techLower.contains("soft") ||
                techLower.contains("presentation")) {
            return "SOFT_SKILLS";
        }
        return "TECHNIQUE";
    }

    /**
     * Récupère une proposition par son ID
     */
    public FormationProposition getPropositionById(String propositionId) {
        return propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée avec l'ID: " + propositionId));
    }

    /**
     * Supprime une proposition
     */
    public void supprimerProposition(String propositionId) {
        FormationProposition proposition = getPropositionById(propositionId);
        propositionRepository.delete(proposition);
        log.info("🗑️ Proposition supprimée: {}", proposition.getTitre());
    }


    // Dans FormationPropositionService.java - AJOUTER CES MÉTHODES

    /**
     * Ouvrir le vote pour une proposition
     */
    public FormationProposition ouvrirVote(String propositionId, int dureeJours, int seuilMinimum) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée avec l'ID: " + propositionId));

        // Vérifier que la proposition existe
        if (proposition == null) {
            throw new RuntimeException("Proposition non trouvée");
        }

        proposition.setStatutVote("OUVERT");
        proposition.setDateFinVote(LocalDateTime.now().plusDays(dureeJours));
        proposition.setSeuilMinimum(seuilMinimum);

        log.info("🔓 Vote ouvert pour la formation '{}' - Durée: {} jours, Seuil minimum: {} votes",
                proposition.getTitre(), dureeJours, seuilMinimum);

        return propositionRepository.save(proposition);
    }

    /**
     * Clôturer le vote pour une proposition
     */
    public FormationProposition cloturerVote(String propositionId) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        proposition.setStatutVote("CLOTURE");
        proposition.calculerStatistiquesVotes();

        log.info("🔒 Vote clôturé pour la formation '{}' - Résultat: {}% pour",
                proposition.getTitre(), proposition.getPourcentagePour());

        return propositionRepository.save(proposition);
    }

    /**
     * Récupérer les statistiques de vote
     */
    public Map<String, Object> getStatsVote(String propositionId) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("propositionId", propositionId);
        stats.put("titre", proposition.getTitre());
        stats.put("totalVotes", proposition.getTotalVotes());
        stats.put("votesPour", proposition.getNbVotesPour());
        stats.put("votesContre", proposition.getNbVotesContre());
        stats.put("votesPeutEtre", proposition.getNbVotesPeutEtre());
        stats.put("pourcentagePour", proposition.getPourcentagePour());
        stats.put("seuilMinimum", proposition.getSeuilMinimum());
        stats.put("seuilAtteint", proposition.isVoteAtteint());
        stats.put("dateFinVote", proposition.getDateFinVote());
        stats.put("statutVote", proposition.getStatutVote());

        return stats;
    }

    /**
     * Vérifier si un employé a déjà voté
     */
    public boolean hasEmployeVoted(String propositionId, String employeId) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        if (proposition.getVotes() == null) {
            return false;
        }

        return proposition.getVotes().stream()
                .anyMatch(v -> v.getEmployeId().equals(employeId));
    }

    /**
     * Enregistrer un vote
     */
    public FormationProposition voter(String propositionId, String employeId, String employeNom,
                                      String employeEmail, String vote, String commentaire) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        // Vérifier si le vote est ouvert
        if (!"OUVERT".equals(proposition.getStatutVote())) {
            throw new RuntimeException("Le vote n'est pas ouvert pour cette proposition");
        }

        // Vérifier si l'employé a déjà voté
        if (hasEmployeVoted(propositionId, employeId)) {
            throw new RuntimeException("Vous avez déjà voté pour cette proposition");
        }

        // Créer le vote
        VoteProposition nouveauVote = new VoteProposition();
        nouveauVote.setEmployeId(employeId);
        nouveauVote.setEmployeNom(employeNom);
        nouveauVote.setEmployeEmail(employeEmail);
        nouveauVote.setVote(vote);
        nouveauVote.setCommentaire(commentaire);
        nouveauVote.setDateVote(LocalDateTime.now());

        if (proposition.getVotes() == null) {
            proposition.setVotes(new ArrayList<>());
        }
        proposition.getVotes().add(nouveauVote);
        proposition.calculerStatistiquesVotes();

        log.info("🗳️ Employé {} a voté '{}' pour la formation '{}'", employeNom, vote, proposition.getTitre());

        return propositionRepository.save(proposition);
    }
}