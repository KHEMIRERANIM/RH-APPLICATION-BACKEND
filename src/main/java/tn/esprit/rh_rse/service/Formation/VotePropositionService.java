// service/Formation/VotePropositionService.java
package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Formation.FormationProposition;
import tn.esprit.rh_rse.entity.Formation.VoteProposition;
import tn.esprit.rh_rse.repository.Formation.FormationPropositionRepository;
import tn.esprit.rh_rse.repository.Formation.VotePropositionRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class VotePropositionService {

    private final FormationPropositionRepository propositionRepository;
    private final VotePropositionRepository voteRepository;

    /**
     * Ouvrir une proposition au vote (Admin)
     */
    public FormationProposition ouvrirVote(String propositionId, int dureeJours, int seuilMinimum) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        proposition.setStatutVote("OUVERT");
        proposition.setDateFinVote(LocalDateTime.now().plusDays(dureeJours));
        proposition.setSeuilMinimum(seuilMinimum);
        proposition.setStatut("VALIDEE");

        log.info("🔓 Vote ouvert pour la formation '{}' - Fin le: {}", proposition.getTitre(), proposition.getDateFinVote());
        return propositionRepository.save(proposition);
    }

    /**
     * Employer vote pour une proposition
     */
    public FormationProposition voter(String propositionId, String employeId, String employeNom,
                                      String employeEmail, String vote, String commentaire) {
        FormationProposition proposition = propositionRepository.findById(propositionId)
                .orElseThrow(() -> new RuntimeException("Proposition non trouvée"));

        // Vérifier si le vote est encore ouvert
        if (!"OUVERT".equals(proposition.getStatutVote())) {
            throw new RuntimeException("Le vote n'est pas ouvert pour cette proposition");
        }

        if (proposition.getDateFinVote() != null && proposition.getDateFinVote().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("La période de vote est terminée");
        }

        // Vérifier si l'employé a déjà voté
        if (hasEmployeVoted(propositionId, employeId)) {
            throw new RuntimeException("Vous avez déjà voté pour cette proposition");
        }

        // Créer le vote
        VoteProposition nouveauVote = new VoteProposition();
        nouveauVote.setPropositionId(propositionId);
        nouveauVote.setEmployeId(employeId);
        nouveauVote.setEmployeNom(employeNom);
        nouveauVote.setEmployeEmail(employeEmail);
        nouveauVote.setVote(vote);
        nouveauVote.setCommentaire(commentaire);
        nouveauVote.setDateVote(LocalDateTime.now());

        // Sauvegarder le vote
        voteRepository.save(nouveauVote);

        // Ajouter le vote à la proposition
        if (proposition.getVotes() == null) {
            proposition.setVotes(new java.util.ArrayList<>());
        }
        proposition.getVotes().add(nouveauVote);
        proposition.calculerStatistiquesVotes();

        proposition = propositionRepository.save(proposition);

        log.info("🗳️ Employé {} a voté '{}' pour la formation '{}'",
                employeNom, vote, proposition.getTitre());

        return proposition;
    }

    /**
     * ✅ Vérifier si un employé a déjà voté
     */
    public boolean hasEmployeVoted(String propositionId, String employeId) {
        return voteRepository.existsByPropositionIdAndEmployeId(propositionId, employeId);
    }

    /**
     * Clôturer le vote (Admin)
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
     * Récupérer toutes les propositions ouvertes au vote
     */
    public List<FormationProposition> getPropositionsOuvertesVote() {
        return propositionRepository.findByStatutAndStatutVote("VALIDEE", "OUVERT");
    }

    /**
     * Récupérer le vote d'un employé (retourne Optional)
     */
    public Optional<VoteProposition> getEmployeVote(String propositionId, String employeId) {
        return voteRepository.findByPropositionIdAndEmployeId(propositionId, employeId);
    }

    /**
     * Récupérer le vote d'un employé (retourne directement l'objet ou null)
     */
    public VoteProposition getEmployeVoteOrNull(String propositionId, String employeId) {
        return voteRepository.findByPropositionIdAndEmployeId(propositionId, employeId).orElse(null);
    }

// Dans FormationPropositionService.java - AJOUTER CETTE MÉTHODE


}