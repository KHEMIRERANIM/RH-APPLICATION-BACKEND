package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.DemandeCongeRequest;
import tn.esprit.rh_rse.dto.request.ValidationCongeRequest;
import tn.esprit.rh_rse.dto.response.DemandeCongeResponse;
import tn.esprit.rh_rse.dto.response.SoldeCongeResponse;
import tn.esprit.rh_rse.entity.DemandeConge;
import tn.esprit.rh_rse.entity.SoldeConge;
import tn.esprit.rh_rse.entity.enums.StatutConge;
import tn.esprit.rh_rse.repository.DemandeCongeRepository;
import tn.esprit.rh_rse.repository.SoldeCongeRepository;
import tn.esprit.rh_rse.service.CongeService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CongeServiceImpl implements CongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final SoldeCongeRepository soldeCongeRepository;

    // ─────────────────────────────────────────────────────────────
    //  EMPLOYE
    // ─────────────────────────────────────────────────────────────

    @Override
    public DemandeCongeResponse soumettreDemande(DemandeCongeRequest request) {
        // Calculer le nombre de jours (sans week-end)
        int nombreJours = calculerJoursOuvrables(request.getDateDebut(), request.getDateFin());

        // Vérifier le solde disponible
        int annee = request.getDateDebut().getYear();
        SoldeConge solde = getOrCreateSolde(request.getEmployeId(), annee);

        if (solde.getJoursRestants() < nombreJours) {
            throw new RuntimeException("Solde de congés insuffisant. Restant: "
                    + solde.getJoursRestants() + " jours, Demandé: " + nombreJours + " jours.");
        }

        // Créer la demande
        DemandeConge demande = DemandeConge.builder()
                .employeId(request.getEmployeId())
                .managerId(request.getManagerId())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .nombreJours(nombreJours)
                .motif(request.getMotif())
                .type(request.getType())
                .statut(StatutConge.EN_ATTENTE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DemandeConge saved = demandeCongeRepository.save(demande);

        // Mettre à jour le solde (jours en attente)
        solde.setJoursEnAttente(solde.getJoursEnAttente() + nombreJours);
        solde.setJoursRestants(solde.getJoursTotal() - solde.getJoursUtilises() - solde.getJoursEnAttente());
        solde.setUpdatedAt(LocalDateTime.now());
        soldeCongeRepository.save(solde);

        return toResponse(saved);
    }

    @Override
    public List<DemandeCongeResponse> getMesDemandes(String employeId) {
        return demandeCongeRepository.findByEmployeId(employeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DemandeCongeResponse getDemandeById(String id) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable avec id: " + id));
        return toResponse(demande);
    }

    @Override
    public void annulerDemande(String id) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable avec id: " + id));

        if (!demande.getStatut().equals(StatutConge.EN_ATTENTE)) {
            throw new RuntimeException("Impossible d'annuler une demande déjà traitée.");
        }

        demande.setStatut(StatutConge.ANNULE);
        demande.setUpdatedAt(LocalDateTime.now());
        demandeCongeRepository.save(demande);

        // Libérer les jours en attente dans le solde
        int annee = demande.getDateDebut().getYear();
        SoldeConge solde = getOrCreateSolde(demande.getEmployeId(), annee);
        solde.setJoursEnAttente(Math.max(0, solde.getJoursEnAttente() - demande.getNombreJours()));
        solde.setJoursRestants(solde.getJoursTotal() - solde.getJoursUtilises() - solde.getJoursEnAttente());
        solde.setUpdatedAt(LocalDateTime.now());
        soldeCongeRepository.save(solde);
    }

    @Override
    public SoldeCongeResponse getSoldeConge(String employeId) {
        int annee = LocalDate.now().getYear();
        SoldeConge solde = getOrCreateSolde(employeId, annee);
        return toSoldeResponse(solde);
    }

    // ─────────────────────────────────────────────────────────────
    //  MANAGER
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<DemandeCongeResponse> getDemandesEnAttente(String managerId) {
        return demandeCongeRepository.findByManagerIdAndStatut(managerId, StatutConge.EN_ATTENTE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DemandeCongeResponse> getToutesDemandesEquipe(String managerId) {
        return demandeCongeRepository.findByManagerId(managerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DemandeCongeResponse validerDemande(String id, ValidationCongeRequest request) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable avec id: " + id));

        if (!demande.getStatut().equals(StatutConge.EN_ATTENTE)) {
            throw new RuntimeException("Cette demande a déjà été traitée.");
        }

        demande.setStatut(request.getStatut());
        demande.setCommentaireManager(request.getCommentaireManager());
        demande.setUpdatedAt(LocalDateTime.now());
        demandeCongeRepository.save(demande);

        // Mettre à jour le solde selon la décision
        int annee = demande.getDateDebut().getYear();
        SoldeConge solde = getOrCreateSolde(demande.getEmployeId(), annee);

        if (request.getStatut().equals(StatutConge.APPROUVE)) {
            solde.setJoursUtilises(solde.getJoursUtilises() + demande.getNombreJours());
        }
        // Dans les deux cas (APPROUVE ou REFUSE), on retire des jours en attente
        solde.setJoursEnAttente(Math.max(0, solde.getJoursEnAttente() - demande.getNombreJours()));
        solde.setJoursRestants(solde.getJoursTotal() - solde.getJoursUtilises() - solde.getJoursEnAttente());
        solde.setUpdatedAt(LocalDateTime.now());
        soldeCongeRepository.save(solde);

        return toResponse(demande);
    }

    // ─────────────────────────────────────────────────────────────
    //  DÉTECTION DE TENDANCES SUSPECTES
    // ─────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> detecterTendances(String managerId) {
        List<DemandeConge> demandes = demandeCongeRepository.findByManagerId(managerId);

        // Grouper par employé
        Map<String, List<DemandeConge>> parEmploye = demandes.stream()
                .filter(d -> d.getStatut().equals(StatutConge.APPROUVE)
                        || d.getStatut().equals(StatutConge.EN_ATTENTE))
                .collect(Collectors.groupingBy(DemandeConge::getEmployeId));

        List<Map<String, Object>> alertes = new ArrayList<>();

        for (Map.Entry<String, List<DemandeConge>> entry : parEmploye.entrySet()) {
            String employeId = entry.getKey();
            List<DemandeConge> demandesEmploye = entry.getValue();

            // Compter les jours de début par jour de la semaine
            Map<DayOfWeek, Long> compteurJours = demandesEmploye.stream()
                    .collect(Collectors.groupingBy(
                            d -> d.getDateDebut().getDayOfWeek(),
                            Collectors.counting()
                    ));

            // Alerte si un jour précis revient 3 fois ou plus
            for (Map.Entry<DayOfWeek, Long> jour : compteurJours.entrySet()) {
                if (jour.getValue() >= 3) {
                    Map<String, Object> alerte = new HashMap<>();
                    alerte.put("employeId", employeId);
                    alerte.put("jourSuspect", jour.getKey().name());
                    alerte.put("nombreOccurrences", jour.getValue());
                    alerte.put("message", "⚠️ L'employé " + employeId
                            + " demande souvent un congé le " + jour.getKey().name()
                            + " (" + jour.getValue() + " fois)");
                    alertes.add(alerte);
                }
            }

            // Alerte si trop de demandes sur une courte période (plus de 5 en 30 jours)
            if (demandesEmploye.size() >= 5) {
                LocalDate today = LocalDate.now();
                long recentCount = demandesEmploye.stream()
                        .filter(d -> d.getDateDebut().isAfter(today.minusDays(30)))
                        .count();
                if (recentCount >= 5) {
                    Map<String, Object> alerte = new HashMap<>();
                    alerte.put("employeId", employeId);
                    alerte.put("type", "FREQUENCE_ELEVEE");
                    alerte.put("nombreDemandes30Jours", recentCount);
                    alerte.put("message", "⚠️ L'employé " + employeId
                            + " a soumis " + recentCount + " demandes dans les 30 derniers jours.");
                    alertes.add(alerte);
                }
            }
        }

        Map<String, Object> resultat = new HashMap<>();
        resultat.put("managerId", managerId);
        resultat.put("nombreAlertes", alertes.size());
        resultat.put("alertes", alertes);
        return resultat;
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTHODES UTILITAIRES PRIVÉES
    // ─────────────────────────────────────────────────────────────

    private SoldeConge getOrCreateSolde(String employeId, int annee) {
        return soldeCongeRepository.findByEmployeIdAndAnnee(employeId, annee)
                .orElseGet(() -> {
                    SoldeConge newSolde = SoldeConge.builder()
                            .employeId(employeId)
                            .annee(annee)
                            .joursTotal(30)
                            .joursUtilises(0)
                            .joursEnAttente(0)
                            .joursRestants(30)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return soldeCongeRepository.save(newSolde);
                });
    }

    private int calculerJoursOuvrables(LocalDate debut, LocalDate fin) {
        int jours = 0;
        LocalDate current = debut;
        while (!current.isAfter(fin)) {
            DayOfWeek jour = current.getDayOfWeek();
            if (jour != DayOfWeek.SATURDAY && jour != DayOfWeek.SUNDAY) {
                jours++;
            }
            current = current.plusDays(1);
        }
        return jours;
    }

    private DemandeCongeResponse toResponse(DemandeConge d) {
        return DemandeCongeResponse.builder()
                .id(d.getId())
                .employeId(d.getEmployeId())
                .managerId(d.getManagerId())
                .dateDebut(d.getDateDebut())
                .dateFin(d.getDateFin())
                .nombreJours(d.getNombreJours())
                .motif(d.getMotif())
                .type(d.getType())
                .statut(d.getStatut())
                .commentaireManager(d.getCommentaireManager())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private SoldeCongeResponse toSoldeResponse(SoldeConge s) {
        return SoldeCongeResponse.builder()
                .employeId(s.getEmployeId())
                .annee(s.getAnnee())
                .joursTotal(s.getJoursTotal())
                .joursUtilises(s.getJoursUtilises())
                .joursEnAttente(s.getJoursEnAttente())
                .joursRestants(s.getJoursRestants())
                .build();
    }
}
