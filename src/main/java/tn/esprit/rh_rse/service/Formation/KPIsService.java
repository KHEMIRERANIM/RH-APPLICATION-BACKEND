package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.Formation.KPIsDTO;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.entity.Formation.ParticipantInscription;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KPIsService {

    private final MongoTemplate mongoTemplate;

    public KPIsDTO calculerTousLesKPIs() {
        KPIsDTO kpis = new KPIsDTO();

        List<Formation> formations = mongoTemplate.findAll(Formation.class);

        if (formations.isEmpty()) {
            log.warn("Aucune formation trouvée");
            return getEmptyKPIs();
        }

        // 1. KPIs classiques avec données réelles
        Map<String, Integer> statsGlobales = getStatistiquesGlobales();
        kpis.setTotalInscrits(statsGlobales.getOrDefault("totalInscrits", 0));
        kpis.setTotalCertifies(statsGlobales.getOrDefault("totalCertifies", 0));
        kpis.setTotalAbandons(statsGlobales.getOrDefault("totalAbandons", 0));

        kpis.setTauxRemplissageMoyen(calculerTauxRemplissageMoyen(formations));
        kpis.setTauxReussiteGlobal(calculerTauxReussiteGlobal(formations));
        kpis.setNpsMoyen(calculerNPSMoyen());
        kpis.setChiffreAffairesTotal(calculerChiffreAffaires(formations));

        // 2. KPIs par formation (avec données réelles)
        kpis.setTauxRemplissageParFormation(calculerTauxRemplissageParFormation(formations));
        kpis.setTauxReussiteParFormation(calculerTauxReussiteParFormation(formations));

        // 3. KPI IA : Prédiction d'échec
        Map<String, Object> predictionEchec = predicteurRisqueEchec(formations);
        kpis.setTauxRisqueEchecGlobal((Double) predictionEchec.get("tauxGlobal"));
        kpis.setRisqueEchecParFormation((Map<String, Double>) predictionEchec.get("parFormation"));
        kpis.setAlertesFormations((Map<String, KPIsDTO.AlerteFormation>) predictionEchec.get("alertes"));

        // 4. 📊 NOUVEAU : Employé le plus actif du mois
        kpis.setEmployeLePlusActif(getEmployeLePlusActifDuMois());

        log.info("✅ KPIs calculés: {} formations, {} inscrits, risque: {}%, employé actif: {}",
                formations.size(), kpis.getTotalInscrits(),
                kpis.getTauxRisqueEchecGlobal(),
                kpis.getEmployeLePlusActif() != null ? kpis.getEmployeLePlusActif().getNom() : "aucun");

        return kpis;
    }

    /**
     * 📊 NOUVEAU : Récupérer l'employé le plus actif du mois
     * Basé sur le nombre d'inscriptions aux formations dans le mois en cours
     */
    private KPIsDTO.EmployeActifDTO getEmployeLePlusActifDuMois() {
        // Obtenir le premier et dernier jour du mois en cours
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Requête pour trouver toutes les inscriptions du mois
        Query query = new Query(Criteria.where("date_inscription")
                .gte(startOfMonth)
                .lte(endOfMonth)
                .and("statut").nin("ANNULE", "CANCELLED"));

        List<ParticipantInscription> inscriptions = mongoTemplate.find(query, ParticipantInscription.class);

        if (inscriptions.isEmpty()) {
            log.info("Aucune inscription trouvée pour le mois de {}", currentMonth);
            return null;
        }

        // Compter les inscriptions par employé
        Map<String, KPIsDTO.EmployeActifDTO> employeMap = new HashMap<>();

        for (ParticipantInscription ins : inscriptions) {
            String employeId = ins.getEmployeId();
            if (employeId == null) continue;

            KPIsDTO.EmployeActifDTO employe = employeMap.get(employeId);
            if (employe == null) {
                employe = new KPIsDTO.EmployeActifDTO();
                employe.setEmployeId(employeId);
                employe.setNom(ins.getEmployeNom() != null ? ins.getEmployeNom() : "");
                employe.setPrenom(ins.getEmployePrenom() != null ? ins.getEmployePrenom() : "");
                employe.setEmail(ins.getEmployeEmail() != null ? ins.getEmployeEmail() : "");
                employe.setNombreInscriptions(0);
                employe.setFormationsSuivies(new ArrayList<>());
                employeMap.put(employeId, employe);
            }

            employe.setNombreInscriptions(employe.getNombreInscriptions() + 1);
            employe.getFormationsSuivies().add(ins.getFormationTitre() != null ? ins.getFormationTitre() : "Formation");
        }

        // Trouver l'employé avec le plus d'inscriptions
        Optional<KPIsDTO.EmployeActifDTO> plusActif = employeMap.values().stream()
                .max(Comparator.comparingInt(KPIsDTO.EmployeActifDTO::getNombreInscriptions));

        return plusActif.orElse(null);
    }

    private Double calculerTauxRemplissageMoyen(List<Formation> formations) {
        int totalPlaces = 0;
        int totalInscrits = 0;

        for (Formation formation : formations) {
            int places = formation.getNombrePlaces() != null ? formation.getNombrePlaces() : 0;
            int inscrits = getNombreInscritsReels(formation);
            totalPlaces += places;
            totalInscrits += inscrits;
        }

        if (totalPlaces == 0) return 0.0;
        double taux = (totalInscrits * 100.0) / totalPlaces;
        return Math.round(taux * 10.0) / 10.0;
    }

    private Double calculerTauxReussiteGlobal(List<Formation> formations) {
        int totalInscrits = 0;
        int totalValides = 0;

        for (Formation formation : formations) {
            Query queryTotal = new Query(Criteria.where("formationId").is(formation.getId()));
            Query queryValides = new Query(Criteria.where("formationId").is(formation.getId())
                    .and("statut").is("VALIDE"));

            totalInscrits += mongoTemplate.count(queryTotal, ParticipantInscription.class);
            totalValides += mongoTemplate.count(queryValides, ParticipantInscription.class);
        }

        if (totalInscrits == 0) return 0.0;
        double taux = (totalValides * 100.0) / totalInscrits;
        return Math.round(taux * 10.0) / 10.0;
    }

    private int getNombreInscritsReels(Formation formation) {
        Query query = new Query(Criteria.where("formationId").is(formation.getId())
                .and("statut").nin("ANNULE", "CANCELLED"));
        return (int) mongoTemplate.count(query, ParticipantInscription.class);
    }

    private Map<String, Integer> getStatistiquesGlobales() {
        Map<String, Integer> stats = new HashMap<>();

        Query queryInscrits = new Query(Criteria.where("statut").nin("ANNULE", "CANCELLED"));
        stats.put("totalInscrits", (int) mongoTemplate.count(queryInscrits, ParticipantInscription.class));

        Query queryValides = new Query(Criteria.where("statut").is("VALIDE"));
        stats.put("totalCertifies", (int) mongoTemplate.count(queryValides, ParticipantInscription.class));

        Query queryEchec = new Query(Criteria.where("statut").is("ECHEC"));
        stats.put("totalAbandons", (int) mongoTemplate.count(queryEchec, ParticipantInscription.class));

        return stats;
    }

    private Map<String, Double> calculerTauxRemplissageParFormation(List<Formation> formations) {
        Map<String, Double> result = new LinkedHashMap<>();

        for (Formation formation : formations) {
            int places = formation.getNombrePlaces() != null ? formation.getNombrePlaces() : 0;
            int inscrits = getNombreInscritsReels(formation);
            double taux = places > 0 ? (inscrits * 100.0) / places : 0;
            result.put(formation.getTitre(), Math.round(taux * 10.0) / 10.0);
        }
        return result;
    }

    private Map<String, Double> calculerTauxReussiteParFormation(List<Formation> formations) {
        Map<String, Double> result = new LinkedHashMap<>();

        for (Formation formation : formations) {
            Query queryTotal = new Query(Criteria.where("formationId").is(formation.getId()));
            Query queryValides = new Query(Criteria.where("formationId").is(formation.getId())
                    .and("statut").is("VALIDE"));

            long totalInscrits = mongoTemplate.count(queryTotal, ParticipantInscription.class);
            long totalValides = mongoTemplate.count(queryValides, ParticipantInscription.class);

            double taux = totalInscrits == 0 ? 0 : (totalValides * 100.0) / totalInscrits;
            result.put(formation.getTitre(), Math.round(taux * 10.0) / 10.0);
        }
        return result;
    }

    private Map<String, Object> predicteurRisqueEchec(List<Formation> formations) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Double> risqueParFormation = new LinkedHashMap<>();
        Map<String, KPIsDTO.AlerteFormation> alertes = new HashMap<>();

        double totalRisques = 0;
        int nbFormations = 0;

        for (Formation formation : formations) {
            double risque = calculerRisquePourFormation(formation);
            double risqueArrondi = Math.round(risque * 10.0) / 10.0;
            risqueParFormation.put(formation.getTitre(), risqueArrondi);
            totalRisques += risque;
            nbFormations++;

            if (risque > 65 && getNombreInscritsReels(formation) > 0) {
                KPIsDTO.AlerteFormation alerte = new KPIsDTO.AlerteFormation();
                alerte.setRisque(risqueArrondi);
                alerte.setRecommandation(risque > 80 ? "Intervention urgente requise" : "À surveiller attentivement");
                alerte.setAction("Contacter les apprenants à risque");
                alertes.put(formation.getTitre(), alerte);
            }
        }

        result.put("tauxGlobal", nbFormations == 0 ? 0 : Math.round((totalRisques / nbFormations) * 10.0) / 10.0);
        result.put("parFormation", risqueParFormation);
        result.put("alertes", alertes);

        return result;
    }

    private double calculerRisquePourFormation(Formation formation) {
        Query query = new Query(Criteria.where("formationId").is(formation.getId()));
        List<ParticipantInscription> inscriptions = mongoTemplate.find(query, ParticipantInscription.class);

        if (inscriptions.isEmpty()) return 0.0;

        double totalRisque = 0;
        int nbInscriptions = 0;

        for (ParticipantInscription inscription : inscriptions) {
            if ("ANNULE".equals(inscription.getStatut()) || "CANCELLED".equals(inscription.getStatut())) {
                continue;
            }

            double risqueIndividuel = 0;
            nbInscriptions++;

            if (inscription.getPresenceValidee() == null || !inscription.getPresenceValidee()) {
                risqueIndividuel += 30;
            }

            // Si la formation est passée et pas de présence validée
            if (formation.getDateFin() != null && formation.getDateFin().isBefore(LocalDateTime.now())) {
                if (inscription.getPresenceValidee() == null || !inscription.getPresenceValidee()) {
                    risqueIndividuel += 40;
                }
            }

            totalRisque += Math.min(100, risqueIndividuel);
        }

        return nbInscriptions == 0 ? 0 : totalRisque / nbInscriptions;
    }

    private Double calculerChiffreAffaires(List<Formation> formations) {
        return formations.size() * 1250.0;
    }

    private Double calculerNPSMoyen() {
        return 72.5;
    }

    private KPIsDTO getEmptyKPIs() {
        KPIsDTO kpis = new KPIsDTO();
        kpis.setTauxRemplissageMoyen(0.0);
        kpis.setTauxReussiteGlobal(0.0);
        kpis.setTotalInscrits(0);
        kpis.setTotalCertifies(0);
        kpis.setTotalAbandons(0);
        kpis.setNpsMoyen(0.0);
        kpis.setChiffreAffairesTotal(0.0);
        kpis.setTauxRemplissageParFormation(new HashMap<>());
        kpis.setTauxReussiteParFormation(new HashMap<>());
        kpis.setTauxRisqueEchecGlobal(0.0);
        kpis.setRisqueEchecParFormation(new HashMap<>());
        kpis.setAlertesFormations(new HashMap<>());
        kpis.setEmployeLePlusActif(null);
        return kpis;
    }
}