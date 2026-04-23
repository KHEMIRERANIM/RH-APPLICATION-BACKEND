package tn.esprit.rh_rse.sched;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.rh_rse.entity.Commande;
import tn.esprit.rh_rse.repository.CommandeRepository;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandeScheduler {

    private final CommandeRepository commandeRepository;

    private static final long DELAI_EXPIRATION_MINUTES = 2;   // suppression après 1h
    private static final long DELAI_ALERTE_MINUTES     = 1;   // alerte préventive à 45min


    @Scheduled(fixedRate = 30_000)
    public void supprimerCommandesExpirees() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(DELAI_EXPIRATION_MINUTES);

        List<Commande> expirees = commandeRepository.findAll().stream()
                .filter(c -> "prete".equals(c.getStatut()))
                .filter(c -> c.getDatePrete() != null && c.getDatePrete().isBefore(limite))
                .toList();

        if (!expirees.isEmpty()) {
            expirees.forEach(c -> {
                log.warn("[Scheduler] SUPPRESSION commande {} — prête depuis {} min — employé {} absent.",
                        c.getId(),
                        Duration.between(c.getDatePrete(), LocalDateTime.now()).toMinutes(),
                        c.getUserId());
                commandeRepository.deleteById(c.getId());
            });
            log.warn("[Scheduler] {} commande(s) expirée(s) supprimée(s).", expirees.size());
        } else {
            log.info("[Scheduler]  Aucune commande expirée à supprimer.");
        }
    }

    /**
     * Toutes les 30 secondes :
     * Alerte si une commande "prete" dépasse 45 min sans récupération
     */
    @Scheduled(fixedRate = 30_000)
    public void alerterCommandesEnRetard() {
        LocalDateTime seuilAlerte = LocalDateTime.now().minusMinutes(DELAI_ALERTE_MINUTES);

        List<Commande> enRetard = commandeRepository.findAll().stream()
                .filter(c -> "prete".equals(c.getStatut()))
                .filter(c -> c.getDatePrete() != null && c.getDatePrete().isBefore(seuilAlerte))
                .toList();

        if (!enRetard.isEmpty()) {
            enRetard.forEach(c ->
                    log.warn("[Scheduler] ⚠ Commande {} — prête depuis {} min — employé {} doit récupérer bientôt !",
                            c.getId(),
                            Duration.between(c.getDatePrete(), LocalDateTime.now()).toMinutes(),
                            c.getUserId())
            );
        } else {
            log.info("[Scheduler] Aucune commande en retard de récupération.");
        }
    }

    /**
     * Rapport toutes les heures
     */
    @Scheduled(fixedRate = 3_600_000)
    public void rapportCommandesEnCours() {
        List<Commande> toutes = commandeRepository.findAll();

        long enAttente  = toutes.stream().filter(c -> "en_attente".equals(c.getStatut())).count();
        long confirmees = toutes.stream().filter(c -> "confirmee".equals(c.getStatut())).count();
        long pretes     = toutes.stream().filter(c -> "prete".equals(c.getStatut())).count();
        long livrees    = toutes.stream().filter(c -> "livree".equals(c.getStatut())).count();

        log.info("[Scheduler]  Rapport — en_attente: {} | confirmee: {} | prete: {} | livree: {}",
                enAttente, confirmees, pretes, livrees);
    }
}