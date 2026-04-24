// ParticipantService.java
package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Formation.ParticipantInscription;
import tn.esprit.rh_rse.repository.Formation.ParticipantInscriptionRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantService {

    private final ParticipantInscriptionRepository participantRepository;


    public List<ParticipantInscription> getInscriptionsByEmploye(String employeId) {
        log.info("📋 Récupération des inscriptions pour l'employé: {}", employeId);
        return participantRepository.findByEmployeId(employeId);
    }

    public List<ParticipantInscription> getParticipantsByFormation(String formationId) {
        log.info("📋 Récupération des participants pour la formation: {}", formationId);
        return participantRepository.findByFormationId(formationId);
    }

    public Optional<ParticipantInscription> getInscription(String formationId, String employeId) {
        return participantRepository.findByFormationIdAndEmployeId(formationId, employeId);
    }

    public ParticipantInscription inscrireParticipant(String formationId, String employeId,
                                                      String nom, String prenom, String email) {
        log.info("📝 Inscription du participant {} à la formation {}", employeId, formationId);

        // Vérifier si déjà inscrit
        Optional<ParticipantInscription> existing = participantRepository.findByFormationIdAndEmployeId(formationId, employeId);
        if (existing.isPresent()) {
            log.warn("⚠️ Participant déjà inscrit: {}", employeId);
            return existing.get();
        }

        ParticipantInscription inscription = new ParticipantInscription();
        inscription.setFormationId(formationId);
        inscription.setEmployeId(employeId);
        inscription.setEmployeNom(nom);
        inscription.setEmployePrenom(prenom);
        inscription.setEmployeEmail(email);
        inscription.setStatut("INSCRIT");
        inscription.setPresenceValidee(false);
        inscription.setDateInscription(LocalDateTime.now());

        return participantRepository.save(inscription);
    }

    public ParticipantInscription validerPresence(String formationId, String employeId, String commentaire) {
        log.info("✅ Validation de présence pour {} à la formation {}", employeId, formationId);

        Optional<ParticipantInscription> optional = participantRepository.findByFormationIdAndEmployeId(formationId, employeId);

        if (optional.isEmpty()) {
            throw new RuntimeException("Participant non inscrit à cette formation");
        }

        ParticipantInscription inscription = optional.get();
        inscription.setPresenceValidee(true);
        inscription.setStatut("PRESENT");
        inscription.setDatePresence(LocalDateTime.now());

        if (commentaire != null && !commentaire.isEmpty()) {
            inscription.setCommentaire(commentaire);
        }

        return participantRepository.save(inscription);
    }

    public void annulerInscription(String formationId, String employeId) {
        log.info("❌ Annulation inscription pour {} à la formation {}", employeId, formationId);

        Optional<ParticipantInscription> optional = participantRepository.findByFormationIdAndEmployeId(formationId, employeId);
        optional.ifPresent(participantRepository::delete);
    }

    public Map<String, Object> getStatistiquesFormation(String formationId) {
        Map<String, Object> stats = new HashMap<>();

        long totalInscrits = participantRepository.countByFormationIdAndStatut(formationId, "INSCRIT");
        long totalPresent = participantRepository.countByFormationIdAndStatut(formationId, "PRESENT");
        long totalValides = participantRepository.countByFormationIdAndStatut(formationId, "VALIDE");
        long presenceValidee = participantRepository.countByFormationIdAndPresenceValidee(formationId, true);

        stats.put("totalInscrits", totalInscrits + totalPresent + totalValides);
        stats.put("totalPresent", totalPresent);
        stats.put("totalValides", totalValides);
        stats.put("presenceValidee", presenceValidee);
        stats.put("tauxPresence", totalInscrits + totalPresent + totalValides > 0 ?
                (presenceValidee * 100.0 / (totalInscrits + totalPresent + totalValides)) : 0);

        return stats;
    }
    // ParticipantService.java (backend)
    public ParticipantInscription marquerAbsent(String formationId, String employeId, String commentaire) {
        log.info("❌ Marquage absent pour {} à la formation {}", employeId, formationId);

        Optional<ParticipantInscription> optional = participantRepository.findByFormationIdAndEmployeId(formationId, employeId);

        if (optional.isEmpty()) {
            throw new RuntimeException("Participant non inscrit à cette formation");
        }

        ParticipantInscription inscription = optional.get();
        inscription.setStatut("ABSENT");
        inscription.setPresenceValidee(false);
        inscription.setCommentaire(commentaire);
        inscription.setUpdatedAt(LocalDateTime.now());

        return participantRepository.save(inscription);
    }

    public ParticipantInscription reinitialiserStatut(String formationId, String employeId) {
        log.info("🔄 Réinitialisation du statut pour {} à la formation {}", employeId, formationId);

        Optional<ParticipantInscription> optional = participantRepository.findByFormationIdAndEmployeId(formationId, employeId);

        if (optional.isEmpty()) {
            throw new RuntimeException("Participant non inscrit à cette formation");
        }

        ParticipantInscription inscription = optional.get();
        inscription.setStatut("CONFIRME");
        inscription.setPresenceValidee(false);
        inscription.setDatePresence(null);
        inscription.setCommentaire(null);
        inscription.setUpdatedAt(LocalDateTime.now());

        return participantRepository.save(inscription);
    }
}