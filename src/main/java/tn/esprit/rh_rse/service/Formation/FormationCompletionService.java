package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.entity.Formation.InscriptionFormation;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;
import tn.esprit.rh_rse.repository.Formation.InscriptionFormationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormationCompletionService {

    private final InscriptionFormationRepository inscriptionRepository;
    private final FormationRepository formationRepository;

    /**
     * Job planifié pour mettre à jour les inscriptions en TERMINE
     * S'exécute toutes les minutes pour les tests (cron: "0 * * * * *")
     * En production, mettre "0 0 * * * *" (toutes les heures)
     */
    @Scheduled(cron = "0 * * * * *") // Toutes les minutes
    @Transactional
    public void updateCompletedFormations() {
        log.info("🔄 Démarrage du job de mise à jour des formations terminées");

        LocalDateTime now = LocalDateTime.now();

        // Récupérer toutes les formations
        List<Formation> formations = formationRepository.findAll();

        int updatedCount = 0;

        for (Formation formation : formations) {
            // Si la formation est terminée (date_fin < maintenant)
            if (formation.getDateFin() != null && formation.getDateFin().isBefore(now)) {
                // Récupérer les inscriptions de cette formation avec statut CONFIRME
                List<InscriptionFormation> inscriptions = inscriptionRepository.findByFormationIdAndStatut(formation.getId(), "CONFIRME");

                for (InscriptionFormation inscription : inscriptions) {
                    inscription.setStatut("TERMINE");
                    inscriptionRepository.save(inscription);
                    updatedCount++;
                    log.info("✅ Formation terminée: {} - Inscription {} mise à jour en TERMINE",
                            formation.getTitre(), inscription.getId());
                }
            }
        }

        if (updatedCount > 0) {
            log.info("✅ Job terminé: {} inscription(s) mise(s) à jour en TERMINE", updatedCount);
        } else {
            log.info("ℹ️ Aucune inscription à mettre à jour");
        }
    }
}