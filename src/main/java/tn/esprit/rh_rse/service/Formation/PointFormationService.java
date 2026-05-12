package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.entity.Formation.PointFormation;
import tn.esprit.rh_rse.entity.Formation.TransactionPoint;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.repository.Formation.PointFormationRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointFormationService {

    private final PointFormationRepository pointRepository;
    private final UserRepository userRepository;
    private static final int POINTS_MENSUELS = 1000;
    private static final int COUT_FORMATION = 1000;

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void rechargerPointsMensuels() {
        log.info("🔄 Démarrage du rechargement mensuel des points");

        List<User> employes = userRepository.findByRole(Role.EMPLOYE);
        int rechargesCount = 0;

        for (User employe : employes) {
            rechargerPointsEmploye(employe.getId(), employe.getNom() + " " + employe.getPrenom());
            rechargesCount++;
        }

        log.info("✅ Rechargement terminé: {} employé(s) rechargé(s)", rechargesCount);
    }

    @Transactional
    public void rechargerPointsEmploye(String employeId, String employeNom) {
        PointFormation points = pointRepository.findByEmployeId(employeId)
                .orElseGet(() -> creerNouveauCompte(employeId, employeNom));

        LocalDateTime maintenant = LocalDateTime.now();

        if (points.getDernierRechargement() == null ||
                ChronoUnit.MONTHS.between(points.getDernierRechargement(), maintenant) >= 1) {

            int ancienSolde = points.getSolde();
            points.setSolde(ancienSolde + POINTS_MENSUELS);
            points.setDernierRechargement(maintenant);

            TransactionPoint transaction = new TransactionPoint();
            transaction.setId(UUID.randomUUID().toString());
            transaction.setType("CREDIT");
            transaction.setMontant(POINTS_MENSUELS);
            transaction.setRaison("Rechargement mensuel");
            transaction.setDate(maintenant);
            transaction.setSoldeApres(points.getSolde());

            if (points.getHistorique() == null) {
                points.setHistorique(new ArrayList<>());
            }
            points.getHistorique().add(transaction);

            points.setUpdatedAt(maintenant);
            pointRepository.save(points);

            log.info("✅ Rechargement de {} points pour {}", POINTS_MENSUELS, employeNom);
        }
    }

    @Transactional
    public void verifierEtRechargerPoints(String employeId, String employeNom) {
        PointFormation points = pointRepository.findByEmployeId(employeId)
                .orElseGet(() -> creerNouveauCompte(employeId, employeNom));

        LocalDateTime maintenant = LocalDateTime.now();

        if (points.getDernierRechargement() == null ||
                ChronoUnit.MONTHS.between(points.getDernierRechargement(), maintenant) >= 1) {
            rechargerPointsEmploye(employeId, employeNom);
        }
    }

    private PointFormation creerNouveauCompte(String employeId, String employeNom) {
        PointFormation points = new PointFormation();
        points.setEmployeId(employeId);
        points.setEmployeNom(employeNom);
        points.setSolde(POINTS_MENSUELS);
        points.setDernierRechargement(LocalDateTime.now());
        points.setHistorique(new ArrayList<>());
        points.setCreatedAt(LocalDateTime.now());
        points.setUpdatedAt(LocalDateTime.now());

        TransactionPoint transaction = new TransactionPoint();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setType("CREDIT");
        transaction.setMontant(POINTS_MENSUELS);
        transaction.setRaison("Création du compte");
        transaction.setDate(LocalDateTime.now());
        transaction.setSoldeApres(POINTS_MENSUELS);
        points.getHistorique().add(transaction);

        return pointRepository.save(points);
    }

    @Transactional
    public void deduirePoints(String employeId, String employeNom, String formationTitre) {
        PointFormation points = pointRepository.findByEmployeId(employeId)
                .orElseThrow(() -> new RuntimeException("Compte de points non trouvé"));

        if (points.getSolde() < COUT_FORMATION) {
            throw new RuntimeException(String.format(
                    "Points insuffisants. Vous avez %d points, besoin de %d points.",
                    points.getSolde(), COUT_FORMATION
            ));
        }

        int ancienSolde = points.getSolde();
        points.setSolde(ancienSolde - COUT_FORMATION);

        TransactionPoint transaction = new TransactionPoint();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setType("DEBIT");
        transaction.setMontant(COUT_FORMATION);
        transaction.setRaison("Inscription à la formation: " + formationTitre);
        transaction.setDate(LocalDateTime.now());
        transaction.setSoldeApres(points.getSolde());

        points.getHistorique().add(transaction);
        points.setUpdatedAt(LocalDateTime.now());

        pointRepository.save(points);

        log.info("💰 Dédution de {} points pour {} (formation: {}). Nouveau solde: {}",
                COUT_FORMATION, employeNom, formationTitre, points.getSolde());
    }

    public int getSoldePoints(String employeId) {
        return pointRepository.findByEmployeId(employeId)
                .map(PointFormation::getSolde)
                .orElse(POINTS_MENSUELS);
    }
    // PointFormationService.java - Ajouter cette méthode
    private static final int BONUS_EXAMEN_EXCELLENT = 500;
    private static final double SEUIL_EXCELLENCE = 15.0;

    @Transactional
    public void attribuerBonusExcellence(String employeId, String employeNom,
                                         String examenTitre, double note) {
        if (note >= SEUIL_EXCELLENCE) {
            // Vérifier si le bonus a déjà été attribué pour cet examen
            String cleBonus = "bonus_examen_excellence_" + employeId + "_" + examenTitre;

            // Option 1: Stocker dans une table séparée ou utiliser l'historique
            // Option simple: vérifier dans l'historique si ce bonus existe déjà
            PointFormation compte = pointRepository.findByEmployeId(employeId).orElse(null);
            if (compte != null && compte.getHistorique() != null) {
                boolean dejaAttribue = compte.getHistorique().stream()
                        .anyMatch(t -> t.getRaison() != null &&
                                t.getRaison().contains("Bonus excellence") &&
                                t.getRaison().contains(examenTitre));

                if (!dejaAttribue) {
                    ajouterPoints(employeId, BONUS_EXAMEN_EXCELLENT,
                            String.format("🎉 Bonus excellence ! +%d points pour note %.1f/20 à l'examen '%s'",
                                    BONUS_EXAMEN_EXCELLENT, note, examenTitre));
                    log.info("🎁 {} points attribués à {} pour note {}/20 à l'examen '{}'",
                            BONUS_EXAMEN_EXCELLENT, employeNom, note, examenTitre);
                } else {
                    log.info("Bonus déjà attribué pour l'examen '{}' à {}", examenTitre, employeNom);
                }
            }
        }
    }
    public List<TransactionPoint> getHistoriquePoints(String employeId) {
        return pointRepository.findByEmployeId(employeId)
                .map(PointFormation::getHistorique)
                .orElse(new ArrayList<>());
    }
    /**
     * Ajouter des points (bonus ou remboursement)
     */

    // PointFormationService.java - Ajouter cette méthode
    @Transactional(readOnly = true)
    public Map<String, Object> debugPoints(String employeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("employeId", employeId);

        Optional<PointFormation> optPoints = pointRepository.findByEmployeId(employeId);
        if (optPoints.isPresent()) {
            PointFormation points = optPoints.get();
            result.put("exists", true);
            result.put("solde", points.getSolde());
            result.put("employeNom", points.getEmployeNom());
            result.put("dernierRechargement", points.getDernierRechargement());
        } else {
            result.put("exists", false);
            result.put("message", "Aucun compte points trouvé pour cet employé");

            // Lister tous les comptes points existants
            List<PointFormation> allPoints = pointRepository.findAll();
            List<Map<String, String>> accounts = new ArrayList<>();
            for (PointFormation p : allPoints) {
                Map<String, String> acc = new HashMap<>();
                acc.put("employeId", p.getEmployeId());
                acc.put("employeNom", p.getEmployeNom());
                acc.put("solde", String.valueOf(p.getSolde()));
                accounts.add(acc);
            }
            result.put("existingAccounts", accounts);
        }

        return result;
    }

    @Transactional
    public void ajouterPoints(String employeId, int points, String raison) {
        log.info("💰 Ajout de {} points pour {}", points, employeId);

        PointFormation compte = pointRepository.findByEmployeId(employeId)
                .orElseGet(() -> {
                    PointFormation nouveau = new PointFormation();
                    nouveau.setEmployeId(employeId);
                    nouveau.setSolde(0);
                    nouveau.setHistorique(new ArrayList<>());
                    return nouveau;
                });

        compte.setSolde(compte.getSolde() + points);

        TransactionPoint transaction = new TransactionPoint();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setType("CREDIT");
        transaction.setMontant(points);
        transaction.setRaison(raison);
        transaction.setDate(LocalDateTime.now());
        transaction.setSoldeApres(compte.getSolde());

        if (compte.getHistorique() == null) {
            compte.setHistorique(new ArrayList<>());
        }
        compte.getHistorique().add(transaction);

        pointRepository.save(compte);
        log.info("✅ Nouveau solde: {}", compte.getSolde());
    }
}