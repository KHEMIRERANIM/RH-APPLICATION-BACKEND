package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.DemandeCongeRequest;
import tn.esprit.rh_rse.dto.request.ValidationCongeRequest;
import tn.esprit.rh_rse.dto.response.DemandeCongeResponse;
import tn.esprit.rh_rse.dto.response.SoldeCongeResponse;
import tn.esprit.rh_rse.entity.DemandeConge;
import tn.esprit.rh_rse.entity.SoldeConge;
import tn.esprit.rh_rse.entity.enums.StatutConge;
import tn.esprit.rh_rse.repository.DemandeCongeRepository;
import tn.esprit.rh_rse.repository.SoldeCongeRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.CongeService;
import tn.esprit.rh_rse.service.EmailService;
import tn.esprit.rh_rse.service.FileStorageService;
import tn.esprit.rh_rse.service.NotificationService;

import java.io.IOException;
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
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;  // ← Ajouter cette ligne

    // ─────────────────────────────────────────────────────────────
    //  EMPLOYE
    // ─────────────────────────────────────────────────────────────

    @Override
    public DemandeCongeResponse soumettreDemande(DemandeCongeRequest request) {
        int nombreJours = calculerJoursOuvrables(request.getDateDebut(), request.getDateFin());

        int annee = request.getDateDebut().getYear();
        SoldeConge solde = getOrCreateSolde(request.getEmployeId(), annee);

        if (solde.getJoursRestants() < nombreJours) {
            throw new RuntimeException("Solde de congés insuffisant. Restant: "
                    + solde.getJoursRestants() + " jours, Demandé: " + nombreJours + " jours.");
        }

        String employeNom = userRepository.findById(request.getEmployeId())
                .map(u -> u.getPrenom() + " " + u.getNom())
                .orElse(request.getEmployeId());

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

        solde.setJoursEnAttente(solde.getJoursEnAttente() + nombreJours);
        solde.setJoursRestants(solde.getJoursTotal() - solde.getJoursUtilises() - solde.getJoursEnAttente());
        solde.setUpdatedAt(LocalDateTime.now());
        soldeCongeRepository.save(solde);

        try {
            notificationService.notifierNouvelleDemande(employeNom, nombreJours);
        } catch (Exception e) {
            System.err.println("❌ Erreur notification: " + e.getMessage());
        }

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

        List<DemandeConge> demandesApprouvees = demandeCongeRepository
                .findByEmployeIdAndStatut(employeId, StatutConge.APPROUVE);

        int joursUtilises = demandesApprouvees.stream()
                .mapToInt(DemandeConge::getNombreJours)
                .sum();

        List<DemandeConge> demandesEnAttente = demandeCongeRepository
                .findByEmployeIdAndStatut(employeId, StatutConge.EN_ATTENTE);

        int joursEnAttente = demandesEnAttente.stream()
                .mapToInt(DemandeConge::getNombreJours)
                .sum();

        SoldeConge solde = soldeCongeRepository.findByEmployeIdAndAnnee(employeId, annee)
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

        solde.setJoursUtilises(joursUtilises);
        solde.setJoursEnAttente(joursEnAttente);
        solde.setJoursRestants(solde.getJoursTotal() - joursUtilises - joursEnAttente);
        solde.setUpdatedAt(LocalDateTime.now());
        soldeCongeRepository.save(solde);

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
        try {
            System.out.println("=== DÉBUT VALIDATION ===");
            System.out.println("ID: " + id);

            DemandeConge demande = demandeCongeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demande introuvable avec id: " + id));

            System.out.println("Statut actuel: " + demande.getStatut());

            if (!demande.getStatut().equals(StatutConge.EN_ATTENTE)) {
                throw new RuntimeException("Cette demande a déjà été traitée.");
            }

            demande.setStatut(request.getStatut());
            demande.setCommentaireManager(request.getCommentaireManager());
            demande.setUpdatedAt(LocalDateTime.now());
            DemandeConge saved = demandeCongeRepository.save(demande);

            System.out.println("✅ Demande sauvegardée");

            // Mettre à jour le solde
            int annee = demande.getDateDebut().getYear();
            SoldeConge solde = getOrCreateSolde(demande.getEmployeId(), annee);

            if (request.getStatut().equals(StatutConge.APPROUVE)) {
                solde.setJoursUtilises(solde.getJoursUtilises() + demande.getNombreJours());
            }
            solde.setJoursEnAttente(Math.max(0, solde.getJoursEnAttente() - demande.getNombreJours()));
            solde.setJoursRestants(solde.getJoursTotal() - solde.getJoursUtilises() - solde.getJoursEnAttente());
            solde.setUpdatedAt(LocalDateTime.now());
            soldeCongeRepository.save(solde);

            System.out.println("✅ Solde mis à jour");

            // Notification WebSocket
            try {
                notificationService.notifierValidationCongé(
                        demande.getEmployeId(),
                        request.getStatut().toString(),
                        request.getCommentaireManager()
                );
                System.out.println("✅ Notification envoyée");
            } catch (Exception e) {
                System.err.println("❌ Erreur notification: " + e.getMessage());
            }

            // Email (commenté temporairement)
             try {
                emailService.envoyerEmailValidation(
                         demande.getEmployeId(),
                         request.getStatut().toString(),
                         request.getCommentaireManager(),
                         demande.getDateDebut(),
                         demande.getDateFin()
                 );
             } catch (Exception e) {
                 System.err.println("❌ Erreur email: " + e.getMessage());
             }

            System.out.println("=== FIN VALIDATION ===");

            return toResponse(saved);

        } catch (Exception e) {
            System.err.println("❌ ERREUR VALIDATION: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }


    }

    // ─────────────────────────────────────────────────────────────
    //  DÉTECTION DE TENDANCES SUSPECTES
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<DemandeCongeResponse> getAllDemandes() {
        return demandeCongeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> detecterTendances(String managerId) {
        List<DemandeConge> demandes = demandeCongeRepository.findByManagerId(managerId);

        Map<String, List<DemandeConge>> parEmploye = demandes.stream()
                .filter(d -> d.getStatut().equals(StatutConge.APPROUVE)
                        || d.getStatut().equals(StatutConge.EN_ATTENTE))
                .collect(Collectors.groupingBy(DemandeConge::getEmployeId));

        List<Map<String, Object>> alertes = new ArrayList<>();

        for (Map.Entry<String, List<DemandeConge>> entry : parEmploye.entrySet()) {
            String employeId = entry.getKey();
            List<DemandeConge> demandesEmploye = entry.getValue();

            Map<DayOfWeek, Long> compteurJours = demandesEmploye.stream()
                    .collect(Collectors.groupingBy(
                            d -> d.getDateDebut().getDayOfWeek(),
                            Collectors.counting()
                    ));

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

    @Override
    public List<DemandeCongeResponse> getAllDemandesEnAttente() {
        return demandeCongeRepository.findByStatut(StatutConge.EN_ATTENTE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void supprimerDemande(String id) {
        if (!demandeCongeRepository.existsById(id)) {
            throw new RuntimeException("Demande introuvable avec id: " + id);
        }
        demandeCongeRepository.deleteById(id);
    }

    @Override
    public DemandeCongeResponse modifierDemande(String id, DemandeCongeRequest request) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable avec id: " + id));

        if (!demande.getStatut().equals(StatutConge.EN_ATTENTE)) {
            throw new RuntimeException("Impossible de modifier une demande déjà traitée.");
        }

        demande.setType(request.getType());
        demande.setDateDebut(request.getDateDebut());
        demande.setDateFin(request.getDateFin());
        demande.setMotif(request.getMotif());

        int nouveauNombreJours = calculerJoursOuvrables(request.getDateDebut(), request.getDateFin());
        int ancienNombreJours = demande.getNombreJours();

        if (nouveauNombreJours != ancienNombreJours) {
            int annee = request.getDateDebut().getYear();
            SoldeConge solde = getOrCreateSolde(demande.getEmployeId(), annee);

            solde.setJoursEnAttente(solde.getJoursEnAttente() - ancienNombreJours + nouveauNombreJours);
            solde.setJoursRestants(solde.getJoursTotal() - solde.getJoursUtilises() - solde.getJoursEnAttente());
            solde.setUpdatedAt(LocalDateTime.now());
            soldeCongeRepository.save(solde);

            demande.setNombreJours(nouveauNombreJours);
        }

        demande.setUpdatedAt(LocalDateTime.now());
        DemandeConge saved = demandeCongeRepository.save(demande);

        return toResponse(saved);
    }

    @Override
    public DemandeCongeResponse soumettreDemandeWithFile(DemandeCongeRequest request, MultipartFile document) {
        // Créer la demande normalement
        DemandeCongeResponse response = soumettreDemande(request);

        // Sauvegarder le fichier si présent
        if (document != null && !document.isEmpty()) {
            try {
                String fileName = fileStorageService.saveFile(document, response.getId());

                // Mettre à jour la demande avec l'URL du document
                DemandeConge demande = demandeCongeRepository.findById(response.getId()).get();
                demande.setDocumentUrl(fileName);
                demande.setDocumentType(document.getContentType());
                demandeCongeRepository.save(demande);

                System.out.println("✅ Fichier sauvegardé: " + fileName);
            } catch (IOException e) {
                System.err.println("❌ Erreur sauvegarde fichier: " + e.getMessage());
            }
        }

        return response;
    }
}