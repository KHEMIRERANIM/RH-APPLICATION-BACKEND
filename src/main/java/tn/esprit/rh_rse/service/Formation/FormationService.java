package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.dto.Formation.FormationDTO;
import tn.esprit.rh_rse.entity.Formation.EvaluationFormation;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.entity.Formation.InscriptionFormation;
import tn.esprit.rh_rse.entity.Formation.ParticipantInscription;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.Formation.EvaluationFormationRepository;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;
import tn.esprit.rh_rse.repository.Formation.InscriptionFormationRepository;
import tn.esprit.rh_rse.repository.Formation.ParticipantInscriptionRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormationService {

    private final FormationRepository formationRepository;
    private final InscriptionFormationRepository inscriptionRepository;
    private final EvaluationFormationRepository evaluationRepository;
  private final UserRepository userRepository;
    private final PointFormationService pointService;  // Ajoutez cette ligne
    private final NotificationFormationService notificationFormationService;
    private final ParticipantInscriptionRepository participantInscriptionRepository;
    @Transactional(readOnly = true)
    public List<FormationDTO> getAllFormations() {
        return formationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FormationDTO> getFormationsDisponibles() {
        return formationRepository.findFormationsDisponibles(LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public void mettreAJourStatutsInscriptions() {
        log.info("🔄 Mise à jour automatique des statuts d'inscriptions");
        LocalDateTime now = LocalDateTime.now();

        // Récupérer toutes les inscriptions non terminées et non annulées
        List<InscriptionFormation> inscriptions = inscriptionRepository.findByStatutNotIn(List.of("TERMINE", "ANNULE"));

        int compteurModifications = 0;

        for (InscriptionFormation inscription : inscriptions) {
            // ⚠️ CHANGEMENT ICI: Utiliser formationId au lieu de getFormation()
            Formation formation = formationRepository.findById(inscription.getFormationId())
                    .orElse(null);

            if (formation == null) continue;

            LocalDateTime dateDebut = formation.getDateDebut();
            LocalDateTime dateFin = formation.getDateFin();

            String ancienStatut = inscription.getStatut();
            String nouveauStatut = ancienStatut;

            // Logique de mise à jour des statuts
            if (dateFin != null && dateFin.isBefore(now)) {
                // Formation terminée
                nouveauStatut = "TERMINE";
            }
            else if (dateDebut != null && dateDebut.isBefore(now) && dateFin != null && dateFin.isAfter(now)) {
                // Formation en cours
                if ("CONFIRME".equals(ancienStatut) || "EN_ATTENTE".equals(ancienStatut)) {
                    nouveauStatut = "PRESENT";
                }
            }
            else if (dateDebut != null && dateDebut.isAfter(now)) {
                // Formation à venir
                if (!"CONFIRME".equals(ancienStatut) && !"EN_ATTENTE".equals(ancienStatut)) {
                    nouveauStatut = "CONFIRME";
                }
            }

            // Appliquer le changement si nécessaire
            if (!nouveauStatut.equals(ancienStatut)) {
                inscription.setStatut(nouveauStatut);
                inscription.setUpdatedAt(LocalDateTime.now());
                compteurModifications++;
                log.info("📝 Inscription {}: {} -> {}", inscription.getId(), ancienStatut, nouveauStatut);
            }
        }

        if (compteurModifications > 0) {
            inscriptionRepository.saveAll(inscriptions);
            log.info("✅ {} inscriptions mises à jour", compteurModifications);
        }
    }
    @Transactional(readOnly = true)
    public FormationDTO getFormationById(String id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));
        return convertToDTO(formation);
    }

    @Transactional
    public FormationDTO createFormation(FormationDTO formationDTO) {
        log.info("Création d'une nouvelle formation: {}", formationDTO.getTitre());

        Formation formation = new Formation();
        formation.setTitre(formationDTO.getTitre());
        formation.setDescription(formationDTO.getDescription());
        formation.setObjectifs(formationDTO.getObjectifs());
        formation.setPreRequis(formationDTO.getPreRequis());
        formation.setType(formationDTO.getType());
        formation.setDureeHeures(formationDTO.getDureeHeures());
        formation.setNombrePlaces(formationDTO.getNombrePlaces());
        formation.setPlacesDisponibles(formationDTO.getNombrePlaces());
        formation.setNiveau(formationDTO.getNiveau());
        formation.setFormateur(formationDTO.getFormateur());
        formation.setFormateurBio(formationDTO.getFormateurBio());
        formation.setLieu(formationDTO.getLieu());
        formation.setLienVisio(formationDTO.getLienVisio());
        formation.setLienGoogleMaps(formationDTO.getLienGoogleMaps());
        formation.setDateDebut(formationDTO.getDateDebut());
        formation.setDateFin(formationDTO.getDateFin());
        formation.setDateLimiteInscription(formationDTO.getDateLimiteInscription());
        formation.setImageUrl(formationDTO.getImageUrl());
        formation.setActive(true);
        formation.setCreatedAt(LocalDateTime.now());
        formation.setUpdatedAt(LocalDateTime.now());

        // ==================== PRÉREQUIS ====================
        formation.setPrerequisFormationId(formationDTO.getPrerequisFormationId());
        formation.setPrerequisFormationTitre(formationDTO.getPrerequisFormationTitre());

        Formation savedFormation = formationRepository.save(formation);
        log.info("Formation créée avec succès: {}", savedFormation.getId());
        return convertToDTO(savedFormation);
    }

    @Transactional
    public FormationDTO updateFormation(String id, FormationDTO formationDTO) {
        log.info("Mise à jour de la formation: {}", id);

        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        // Sauvegarder les anciennes dates pour comparaison (avec gestion des null)
        LocalDateTime ancienneDateDebut = formation.getDateDebut();
        LocalDateTime ancienneDateFin = formation.getDateFin();

        // Mettre à jour la formation
        formation.setTitre(formationDTO.getTitre());
        formation.setDescription(formationDTO.getDescription());
        formation.setObjectifs(formationDTO.getObjectifs());
        formation.setPreRequis(formationDTO.getPreRequis());
        formation.setType(formationDTO.getType());
        formation.setDureeHeures(formationDTO.getDureeHeures());
        formation.setNombrePlaces(formationDTO.getNombrePlaces());
        formation.setPlacesDisponibles(formationDTO.getPlacesDisponibles());
        formation.setNiveau(formationDTO.getNiveau());
        formation.setFormateur(formationDTO.getFormateur());
        formation.setFormateurBio(formationDTO.getFormateurBio());
        formation.setLieu(formationDTO.getLieu());
        formation.setLienVisio(formationDTO.getLienVisio());
        formation.setLienGoogleMaps(formationDTO.getLienGoogleMaps());
        formation.setDateDebut(formationDTO.getDateDebut());
        formation.setDateFin(formationDTO.getDateFin());
        formation.setDateLimiteInscription(formationDTO.getDateLimiteInscription());
        formation.setImageUrl(formationDTO.getImageUrl());
        formation.setUpdatedAt(LocalDateTime.now());

        formation.setPrerequisFormationId(formationDTO.getPrerequisFormationId());
        formation.setPrerequisFormationTitre(formationDTO.getPrerequisFormationTitre());

        Formation updatedFormation = formationRepository.save(formation);

        // 🔥 CRITIQUE: Recalculer les statuts des inscriptions si les dates ont changé
        // Utilisation de Objects.equals pour gérer les null
        boolean datesModifiees = !Objects.equals(ancienneDateDebut, formationDTO.getDateDebut()) ||
                !Objects.equals(ancienneDateFin, formationDTO.getDateFin());

        if (datesModifiees) {
            log.info("📅 Dates modifiées - Anciennes: début={}, fin={} | Nouvelles: début={}, fin={}",
                    ancienneDateDebut, ancienneDateFin, formationDTO.getDateDebut(), formationDTO.getDateFin());
            mettreAJourStatutsInscriptions();
        } else {
            log.info("📅 Aucune modification des dates");
        }

        log.info("Formation mise à jour avec succès: {}", updatedFormation.getId());
        return convertToDTO(updatedFormation);
    }
    // Dans FormationService.java
    @Transactional
    public void deleteFormation(String id) {
        log.info("🗑️ Suppression de la formation: {}", id);

        // 1. Récupérer la formation
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        // 2. Récupérer toutes les inscriptions pour cette formation
        List<InscriptionFormation> inscriptions = inscriptionRepository.findByFormationId(id);
        List<ParticipantInscription> participantInscriptions = participantInscriptionRepository.findByFormationId(id);

        log.info("📊 Nombre d'inscriptions à traiter: {} (InscriptionFormation) + {} (ParticipantInscription)",
                inscriptions.size(), participantInscriptions.size());

        // 3. Traiter chaque inscription pour remboursement et notification
        for (InscriptionFormation inscription : inscriptions) {
            traiterAnnulationInscription(inscription, formation);
        }

        for (ParticipantInscription participantInscription : participantInscriptions) {
            traiterAnnulationParticipantInscription(participantInscription, formation);
        }

        // 4. Supprimer toutes les inscriptions
        if (!inscriptions.isEmpty()) {
            inscriptionRepository.deleteAll(inscriptions);
            log.info("✅ {} inscriptions (InscriptionFormation) supprimées", inscriptions.size());
        }

        if (!participantInscriptions.isEmpty()) {
            participantInscriptionRepository.deleteAll(participantInscriptions);
            log.info("✅ {} inscriptions (ParticipantInscription) supprimées", participantInscriptions.size());
        }

        // 5. Supprimer la formation
        formationRepository.deleteById(id);
        log.info("✅ Formation supprimée avec succès: {}", id);
    }
    private void traiterAnnulationInscription(InscriptionFormation inscription, Formation formation) {
        try {
            String employeId = inscription.getEmployeId();
            User employe = userRepository.findById(employeId).orElse(null);

            if (employe == null) {
                log.warn("⚠️ Employé {} non trouvé pour l'inscription {}", employeId, inscription.getId());
                return;
            }

            // Vérifier si l'inscription était confirmée
            boolean estConfirmee = "CONFIRME".equals(inscription.getStatut());

            if (estConfirmee) {
                // Rembourser les points (1000 points)
                int pointsARembourser = 1000;
                pointService.ajouterPoints(employeId, pointsARembourser,
                        "Remboursement suite à suppression de la formation: " + formation.getTitre());
                log.info("💰 Remboursement de {} points pour {} - Formation: {}",
                        pointsARembourser, employe.getEmail(), formation.getTitre());
            }

            // ✅ CORRECTION: Utiliser notificationService au lieu d'appeler directement
            notificationFormationService.envoyerEmailSuppressionFormation(employe, formation, estConfirmee);

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de l'inscription {}: {}", inscription.getId(), e.getMessage());
        }
    }

    private void traiterAnnulationParticipantInscription(ParticipantInscription inscription, Formation formation) {
        try {
            String employeId = inscription.getEmployeId();
            User employe = userRepository.findById(employeId).orElse(null);

            if (employe == null) {
                log.warn("⚠️ Employé {} non trouvé pour l'inscription {}", employeId, inscription.getId());
                return;
            }

            // Vérifier si l'inscription était confirmée
            boolean estConfirmee = "CONFIRME".equals(inscription.getStatut());

            if (estConfirmee) {
                // Rembourser les points (1000 points)
                int pointsARembourser = 1000;
                pointService.ajouterPoints(employeId, pointsARembourser,
                        "Remboursement suite à suppression de la formation: " + formation.getTitre());
                log.info("💰 Remboursement de {} points pour {} - Formation: {}",
                        pointsARembourser, employe.getEmail(), formation.getTitre());
            }

            // ✅ CORRECTION: Utiliser notificationService au lieu d'appeler directement
            notificationFormationService.envoyerEmailSuppressionFormationParticipant(employe, formation, estConfirmee);

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de l'inscription participant {}: {}", inscription.getId(), e.getMessage());
        }
    }




    @Transactional
    public void toggleActive(String id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));
        formation.setActive(!formation.getActive());
        formation.setUpdatedAt(LocalDateTime.now());
        formationRepository.save(formation);
        log.info("Statut de la formation {} modifié: active={}", id, formation.getActive());
    }

    private FormationDTO convertToDTO(Formation formation) {
        FormationDTO dto = new FormationDTO();
        dto.setId(formation.getId());
        dto.setTitre(formation.getTitre());
        dto.setDescription(formation.getDescription());
        dto.setObjectifs(formation.getObjectifs());
        dto.setPreRequis(formation.getPreRequis());
        dto.setType(formation.getType());
        dto.setDureeHeures(formation.getDureeHeures());
        dto.setNombrePlaces(formation.getNombrePlaces());
        dto.setPlacesDisponibles(formation.getPlacesDisponibles());
        dto.setNiveau(formation.getNiveau());
        dto.setFormateur(formation.getFormateur());
        dto.setFormateurBio(formation.getFormateurBio());
        dto.setLieu(formation.getLieu());
        dto.setLienVisio(formation.getLienVisio());
        dto.setLienGoogleMaps(formation.getLienGoogleMaps());
        dto.setDateDebut(formation.getDateDebut());
        dto.setDateFin(formation.getDateFin());
        dto.setDateLimiteInscription(formation.getDateLimiteInscription());
        dto.setImageUrl(formation.getImageUrl());
        dto.setActive(formation.getActive());

        // ==================== PRÉREQUIS ====================
        dto.setPrerequisFormationId(formation.getPrerequisFormationId());
        dto.setPrerequisFormationTitre(formation.getPrerequisFormationTitre());

        // Calculer la note moyenne
        List<EvaluationFormation> evaluations = evaluationRepository.findByFormationId(formation.getId());
        double moyenne = evaluations.stream()
                .mapToInt(EvaluationFormation::getNote)
                .average()
                .orElse(0.0);
        dto.setNoteMoyenne(Math.round(moyenne * 10) / 10.0);

        // Compter les inscriptions confirmées
        int nbInscrits = inscriptionRepository.countInscriptionsConfirmees(formation.getId());
        dto.setNombreInscrits(nbInscrits);

        return dto;
    }

    public List<FormationDTO> getFormationsByType(String type) {
        log.info("Recherche des formations de type: {}", type);
        return formationRepository.findByType(type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<FormationDTO> getFormationsTerminees() {
        log.info("📋 Récupération des formations terminées");
        LocalDateTime now = LocalDateTime.now();
        List<Formation> formations = formationRepository.findByDateFinBefore(now);
        return formations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<FormationDTO> searchFormations(String keyword) {
        log.info("Recherche de formations avec mot-clé: {}", keyword);
        return formationRepository.searchFormations(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void setTermine(String inscriptionId) {
        InscriptionFormation inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));
        inscription.setStatut("TERMINE");
        inscriptionRepository.save(inscription);
        log.info("Inscription {} mise à jour en TERMINE", inscriptionId);
    }
}