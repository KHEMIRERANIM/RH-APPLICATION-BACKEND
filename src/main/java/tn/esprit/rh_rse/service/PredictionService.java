package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.response.PredictionResponse;
import tn.esprit.rh_rse.dto.response.RecommandationEmployeResponse;
import tn.esprit.rh_rse.entity.DemandeConge;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.entity.enums.StatutConge;
import tn.esprit.rh_rse.repository.DemandeCongeRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final DemandeCongeRepository demandeRepository;
    private final UserRepository userRepository;

    // ==================== ADMIN PREDICTION ====================

    public PredictionResponse predireChargeMois(int mois, int annee) {
        log.info("Prédiction pour mois={}/{}", mois, annee);

        long demandesAnnee1 = compterDemandesPourMois(annee - 1, mois);
        long demandesAnnee2 = compterDemandesPourMois(annee - 2, mois);
        long totalDemandes = getTotalDemandesApprouvees();

        log.info("Demandes année {}: {}, année {}: {}", annee-1, demandesAnnee1, annee-2, demandesAnnee2);

        int scoreSaison = getScoreSaisonnier(mois);
        int scoreSolde = getScoreSolde();
        int scorePonts = getScorePonts(mois, annee);
        int scoreHistorique = getScoreHistorique(demandesAnnee1, demandesAnnee2);

        int scoreFinal;
        if (totalDemandes < 20) {
            scoreFinal = (int) ((scoreSaison * 0.50) + (scoreSolde * 0.30) + (scorePonts * 0.20));
            log.info("Mode 'peu de données' - utilisation règles métier");
        } else {
            scoreFinal = (int) ((scoreHistorique * 0.40) + (scoreSaison * 0.30) + (scoreSolde * 0.20) + (scorePonts * 0.10));
        }

        scoreFinal = Math.min(scoreFinal, 100);

        List<PredictionResponse.FacteurPrediction> facteurs = new ArrayList<>();
        facteurs.add(PredictionResponse.FacteurPrediction.builder()
                .nom("📊 Historique")
                .impact(scoreHistorique)
                .description(demandesAnnee1 + " demandes en " + (annee-1) + ", " + demandesAnnee2 + " en " + (annee-2))
                .build());
        facteurs.add(PredictionResponse.FacteurPrediction.builder()
                .nom("🌤️ Saison")
                .impact(scoreSaison)
                .description(getDescriptionSaison(mois))
                .build());
        facteurs.add(PredictionResponse.FacteurPrediction.builder()
                .nom("💰 Soldes")
                .impact(scoreSolde)
                .description(getDescriptionSolde())
                .build());
        facteurs.add(PredictionResponse.FacteurPrediction.builder()
                .nom("📅 Ponts")
                .impact(scorePonts)
                .description(getDescriptionPonts(mois, annee))
                .build());

        String risque = scoreFinal >= 80 ? "CRITIQUE" : scoreFinal >= 60 ? "ÉLEVÉ" : scoreFinal >= 40 ? "MOYEN" : "FAIBLE";

        return PredictionResponse.builder()
                .pourcentage(scoreFinal)
                .risque(risque)
                .message(genererMessage(scoreFinal, mois))
                .recommandation(genererRecommandation(scoreFinal))
                .mois(mois)
                .annee(annee)
                .facteurs(facteurs)
                .totalDemandesHistorique((int) totalDemandes)
                .build();
    }

    public List<PredictionResponse> predireTendances() {
        LocalDate maintenant = LocalDate.now();
        List<PredictionResponse> tendances = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            LocalDate date = maintenant.plusMonths(i);
            tendances.add(predireChargeMois(date.getMonthValue(), date.getYear()));
        }
        return tendances;
    }

    private long compterDemandesPourMois(int annee, int mois) {
        if (annee < 2022) return 5;

        return demandeRepository.findAll().stream()
                .filter(d -> d.getStatut() == StatutConge.APPROUVE)
                .filter(d -> d.getDateDebut().getYear() == annee)
                .filter(d -> d.getDateDebut().getMonthValue() == mois)
                .count();
    }

    private long getTotalDemandesApprouvees() {
        return demandeRepository.findAll().stream()
                .filter(d -> d.getStatut() == StatutConge.APPROUVE)
                .count();
    }

    private int getScoreSaisonnier(int mois) {
        Map<Integer, Integer> scores = new HashMap<>();
        scores.put(1, 15);
        scores.put(2, 20);
        scores.put(3, 25);
        scores.put(4, 40);
        scores.put(5, 65);
        scores.put(6, 75);
        scores.put(7, 95);
        scores.put(8, 90);
        scores.put(9, 35);
        scores.put(10, 30);
        scores.put(11, 25);
        scores.put(12, 70);

        return scores.getOrDefault(mois, 50);
    }

    private int getScoreHistorique(long annee1, long annee2) {
        if (annee1 == 0 && annee2 == 0) return 30;
        if (annee2 == 0) return (int) (annee1 * 2);
        double moyenne = (annee1 + annee2) / 2.0;
        double augmentation = annee2 > 0 ? ((annee1 - annee2) / (double) annee2) * 100 : 0;

        int score = (int) (moyenne * 2);
        if (augmentation > 20) score += 15;
        else if (augmentation > 10) score += 10;
        else if (augmentation < -10) score -= 10;

        return Math.min(score, 95);
    }

    private int getScoreSolde() {
        List<User> employes = userRepository.findByRole(Role.EMPLOYE);
        if (employes.isEmpty()) return 30;

        long employesSoldeEleve = employes.stream()
                .filter(e -> getJoursRestantsSimule(e) > 15)
                .count();

        int pourcentage = (int) (employesSoldeEleve * 100 / employes.size());

        log.info("Score solde: {} employés avec >15 jours sur {} total ({}%)",
                employesSoldeEleve, employes.size(), pourcentage);

        return pourcentage;
    }

    private int getJoursRestantsSimule(User employe) {
        int joursPris = (int) demandeRepository.findAll().stream()
                .filter(d -> d.getEmployeId().equals(employe.getId()))
                .filter(d -> d.getStatut() == StatutConge.APPROUVE)
                .mapToInt(DemandeConge::getNombreJours)
                .sum();
        return Math.max(0, 30 - joursPris);
    }

    private int getScorePonts(int mois, int annee) {
        List<LocalDate> joursFeries = getJoursFeries(annee);
        int ponts = 0;

        for (LocalDate ferie : joursFeries) {
            if (ferie.getMonthValue() == mois) {
                if (estJourOuvre(ferie.minusDays(1))) ponts++;
                if (estJourOuvre(ferie.plusDays(1))) ponts++;
            }
        }
        return Math.min(ponts * 35, 100);
    }

    private List<LocalDate> getJoursFeries(int annee) {
        List<LocalDate> feries = new ArrayList<>();
        feries.add(LocalDate.of(annee, 1, 1));
        feries.add(LocalDate.of(annee, 5, 1));
        feries.add(LocalDate.of(annee, 7, 14));
        feries.add(LocalDate.of(annee, 12, 25));
        return feries;
    }

    private boolean estJourOuvre(LocalDate date) {
        return date.getDayOfWeek().getValue() <= 5;
    }

    private String getDescriptionSaison(int mois) {
        String[] descriptions = {
                "Janvier: Début d'année calme", "Février: Calme", "Mars: Reprise progressive",
                "Avril: Début des ponts", "Mai: Mois des ponts", "Juin: Pré-été actif",
                "Juillet: PLEIN ÉTÉ - Période de pointe", "Août: Très chargé", "Septembre: Rentrée calme",
                "Octobre: Calme", "Novembre: Avant les fêtes", "Décembre: Période des fêtes"
        };
        return descriptions[mois - 1];
    }

    private String getDescriptionSolde() {
        // ✅ CORRECTION : Ne prendre que les EMPLOYES
        List<User> employes = userRepository.findByRole(Role.EMPLOYE);
        long count = employes.stream().filter(e -> getJoursRestantsSimule(e) > 15).count();
        if (count > 0) {
            return count + " employé(s) ont plus de 15 jours restants";
        }
        return "Soldes équilibrés";
    }

    private String getDescriptionPonts(int mois, int annee) {
        List<LocalDate> joursFeries = getJoursFeries(annee);
        long ponts = joursFeries.stream()
                .filter(f -> f.getMonthValue() == mois)
                .filter(f -> estJourOuvre(f.minusDays(1)) || estJourOuvre(f.plusDays(1)))
                .count();

        if (ponts > 0) {
            return ponts + " pont(s) possible(s) ce mois-ci";
        }
        return "Aucun pont ce mois";
    }

    private String genererMessage(int score, int mois) {
        String nomMois = LocalDate.of(2000, mois, 1).format(DateTimeFormatter.ofPattern("MMMM"));
        if (score >= 80) {
            return "🚨 Période de TRÈS FORTE demande en " + nomMois + " !";
        } else if (score >= 60) {
            return "⚠️ Période de forte demande attendue en " + nomMois;
        } else if (score >= 40) {
            return "📊 Période de demande modérée en " + nomMois;
        }
        return "✅ Période calme en " + nomMois;
    }

    private String genererRecommandation(int score) {
        if (score >= 80) {
            return "Activez le traitement accéléré des demandes et informez les managers";
        } else if (score >= 60) {
            return "Prévoyez une capacité de validation supplémentaire cette semaine";
        } else if (score >= 40) {
            return "Surveillance normale - Aucune action immédiate requise";
        }
        return "Traitement standard - Profitez de la période calme";
    }

    // ==================== EMPLOYEE RECOMMENDATIONS ====================

    public List<RecommandationEmployeResponse> getRecommandationsEmploye(String employeId) {
        List<RecommandationEmployeResponse> recommandations = new ArrayList<>();

        User employe = userRepository.findById(employeId).orElse(null);
        if (employe == null) return recommandations;

        int joursRestants = getJoursRestantsSimule(employe);

        if (joursRestants > 15) {
            recommandations.add(RecommandationEmployeResponse.builder()
                    .type("SOLDE_ELEVE")
                    .icon("💰")
                    .titre("Solde de congés élevé")
                    .description("Il vous reste " + joursRestants + " jours. Pensez à les poser !")
                    .build());
        }

        LocalDate periodeCalme = trouverPeriodeCalme();
        if (periodeCalme != null) {
            recommandations.add(RecommandationEmployeResponse.builder()
                    .type("PERIODE_CALME")
                    .icon("🍃")
                    .titre("Période calme détectée")
                    .description("La semaine du " + periodeCalme.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " est peu demandée")
                    .dateSuggestion(periodeCalme)
                    .build());
        }

        return recommandations;
    }

    private LocalDate trouverPeriodeCalme() {
        LocalDate maintenant = LocalDate.now();
        for (int i = 1; i <= 6; i++) {
            LocalDate date = maintenant.plusMonths(i);
            long demandes = compterDemandesPourMois(date.getYear(), date.getMonthValue());
            if (demandes < 5) {
                return date.withDayOfMonth(1);
            }
        }
        return null;
    }
}