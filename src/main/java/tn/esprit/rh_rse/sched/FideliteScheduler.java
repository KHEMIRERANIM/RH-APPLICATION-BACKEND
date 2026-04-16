package tn.esprit.rh_rse.sched;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.rh_rse.entity.Fidelite;
import tn.esprit.rh_rse.repository.FideliteRepository;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FideliteScheduler {

    private final FideliteRepository fideliteRepository;

    /**
     * Toutes les 30 secondes (démo) :
     * Expire les points des comptes inactifs depuis 6 mois
     * → points remis à 0, reductionDisponible = false
     * En production : cron = "0 0 8 1 * *" (1er du mois à 8h)
     */
    @Scheduled(fixedRate = 30_000)
    public void expirerPointsInactifs() {
        LocalDate limite = LocalDate.now().minusMonths(6);

        List<Fidelite> aExpirer = fideliteRepository.findAll()
                .stream()
                .filter(f -> f.getPoints() > 0)
                .filter(f -> {
                    // Vérifie si le dernier historique date de plus de 6 mois
                    if (f.getHistorique() == null || f.getHistorique().isEmpty()) {
                        return true; // aucune activité → expirer
                    }
                    String dernierEvent = f.getHistorique().get(f.getHistorique().size() - 1);
                    // format : "+X pts le 2025-10-01 (...)"
                    try {
                        String dateStr = dernierEvent.split("le ")[1].split(" ")[0];
                        LocalDate dernierDate = LocalDate.parse(dateStr);
                        return dernierDate.isBefore(limite);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();

        aExpirer.forEach(f -> {
            log.warn("[Scheduler] Points expirés pour user {} : {} pts remis à 0",
                    f.getUserId(), f.getPoints());
            f.getHistorique().add("Points expirés (inactivité) le " + LocalDate.now());
            f.setPoints(0);
            f.setReductionDisponible(false);
        });

        fideliteRepository.saveAll(aExpirer);

        log.info("[Scheduler] Expiration fidélité : {} compte(s) traité(s)", aExpirer.size());
    }

    /**
     * Toutes les heures :
     * Rapport global des points fidélité
     */
    @Scheduled(fixedRate = 3_600_000)
    public void rapportFidelite() {
        List<Fidelite> toutes = fideliteRepository.findAll();

        long avecReduction = toutes.stream().filter(Fidelite::isReductionDisponible).count();
        int totalPoints    = toutes.stream().mapToInt(Fidelite::getPoints).sum();

        log.info("[Scheduler] Fidélité — {} compte(s) | {} réduction(s) disponible(s) | {} pts total",
                toutes.size(), avecReduction, totalPoints);
    }
}