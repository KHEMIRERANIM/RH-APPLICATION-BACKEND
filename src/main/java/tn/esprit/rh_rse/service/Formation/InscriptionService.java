package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.dto.Formation.InscriptionFormationDTO;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.entity.Formation.InscriptionFormation;
import tn.esprit.rh_rse.entity.Formation.ParticipantInscription;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;
import tn.esprit.rh_rse.repository.Formation.InscriptionFormationRepository;
import tn.esprit.rh_rse.repository.Formation.ParticipantInscriptionRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InscriptionService {

    private final InscriptionFormationRepository inscriptionRepository;
    private final ParticipantInscriptionRepository participantRepository;
    private final FormationRepository formationRepository;
    private final NotificationFormationService notificationFormationService;
    private final UserRepository userRepository;
    private final PointFormationService pointService;

    // ==================== MÉTHODES EXISTANTES AVEC InscriptionFormation ====================

    @Transactional
    public InscriptionFormationDTO inscrireEmployeDTO(String formationId, String employeId) {
        log.info("Tentative d'inscription - formationId: {}, employeId: {}", formationId, employeId);

        User employe = userRepository.findById(employeId)
                .orElseThrow(() -> {
                    log.error("❌ Employé non trouvé avec ID: {}", employeId);
                    return new RuntimeException("Employé non trouvé avec ID: " + employeId);
                });

        log.info("✅ Employé trouvé: {} {} (ID: {})", employe.getPrenom(), employe.getNom(), employe.getId());

        if (inscriptionRepository.existsByFormationIdAndEmployeId(formationId, employeId)) {
            throw new RuntimeException("Vous êtes déjà inscrit à cette formation");
        }

        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        if (formation.getPlacesDisponibles() <= 0) {
            throw new RuntimeException("Plus de places disponibles");
        }

        String employeNom = employe.getNom() + " " + employe.getPrenom();
        pointService.verifierEtRechargerPoints(employeId, employeNom);
        int pointsActuels = pointService.getSoldePoints(employeId);
        int coutFormation = 1000;

        log.info("💰 Points pour {}: {} (besoin: {})", employeNom, pointsActuels, coutFormation);

        if (pointsActuels < coutFormation) {
            throw new RuntimeException(String.format(
                    "Points insuffisants. Vous avez %d points, besoin de %d points.",
                    pointsActuels, coutFormation
            ));
        }

        InscriptionFormation inscription = new InscriptionFormation();
        inscription.setFormationId(formationId);
        inscription.setEmployeId(employeId);
        inscription.setEmployeNom(employeNom);
        inscription.setStatut("EN_ATTENTE");
        inscription.setDateInscription(LocalDateTime.now());
        inscription.setPresenceConfirmee(false);

        inscription.setFormationTitre(formation.getTitre());
        inscription.setFormateur(formation.getFormateur());
        inscription.setLieu(formation.getLieu());
        inscription.setLienVisio(formation.getLienVisio());
        inscription.setLienGoogleMaps(formation.getLienGoogleMaps());
        inscription.setDateDebut(formation.getDateDebut());
        inscription.setDateFin(formation.getDateFin());
        inscription.setDureeHeures(formation.getDureeHeures());

        InscriptionFormation savedInscription = inscriptionRepository.save(inscription);
        log.info("✅ Inscription créée avec succès - ID: {}", savedInscription.getId());

        formation.setPlacesDisponibles(formation.getPlacesDisponibles() - 1);
        formationRepository.save(formation);

        notificationFormationService.envoyerConfirmationInscription(employeId, formation.getTitre());

        return convertToDTO(savedInscription);
    }

    @Transactional
    public void annulerInscription(String inscriptionId, String motif) {
        log.info("Tentative d'annulation de l'inscription {}", inscriptionId);

        InscriptionFormation inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        Formation formation = formationRepository.findById(inscription.getFormationId())
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        User employe = userRepository.findById(inscription.getEmployeId())
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        String employeNom = employe.getNom() + " " + employe.getPrenom();

        if ("CONFIRME".equals(inscription.getStatut())) {
            int coutFormation = 1000;
            pointService.ajouterPoints(inscription.getEmployeId(), coutFormation,
                    "Annulation de la formation: " + formation.getTitre());
            log.info("💰 Remboursement de {} points pour {} (annulation de: {})",
                    coutFormation, employeNom, formation.getTitre());
        }

        inscription.setStatut("ANNULE");
        inscription.setMotifAnnulation(motif);
        inscriptionRepository.save(inscription);

        formation.setPlacesDisponibles(formation.getPlacesDisponibles() + 1);
        formationRepository.save(formation);

        List<Formation> formationsDependantes = formationRepository.findByPrerequisFormationId(formation.getId());

        for (Formation formationDependante : formationsDependantes) {
            List<InscriptionFormation> inscriptionsDependantes = inscriptionRepository.findByFormationIdAndEmployeId(
                    formationDependante.getId(), employe.getId());

            for (InscriptionFormation inscDependante : inscriptionsDependantes) {
                if (!"ANNULE".equals(inscDependante.getStatut())) {
                    if ("CONFIRME".equals(inscDependante.getStatut())) {
                        pointService.ajouterPoints(inscription.getEmployeId(), 1000,
                                "Annulation en cascade: " + formationDependante.getTitre());
                    }

                    inscDependante.setStatut("ANNULE");
                    inscDependante.setMotifAnnulation("Annulation en cascade : la formation prérequise '" +
                            formation.getTitre() + "' a été annulée");
                    inscriptionRepository.save(inscDependante);
                    log.info("📢 Annulation en cascade : {} annulée", formationDependante.getTitre());

                    notificationFormationService.envoyerAnnulationCascade(
                            employe.getEmail(),
                            employe.getNom(),
                            employe.getPrenom(),
                            formationDependante.getTitre(),
                            formation.getTitre(),
                            motif
                    );
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        String dateDebut = formation.getDateDebut().format(formatter);
        String dateFin = formation.getDateFin().format(formatter);

        notificationFormationService.envoyerAnnulationInscription(
                employe.getEmail(),
                employe.getNom(),
                employe.getPrenom(),
                formation.getTitre(),
                dateDebut,
                dateFin,
                formation.getFormateur(),
                formation.getLieu(),
                formation.getType(),
                motif,
                formation.getId()
        );

        log.info("Inscription annulée avec succès pour l'employé {} à la formation {}", employe.getEmail(), formation.getTitre());
    }

    @Transactional(readOnly = true)
    public List<InscriptionFormationDTO> getInscriptionsByEmploye(String employeId) {
        log.info("Récupération des inscriptions pour l'employé {}", employeId);
        return inscriptionRepository.findByEmployeId(employeId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InscriptionFormationDTO> getInscriptionsByFormation(String formationId) {
        log.info("Récupération des inscriptions pour la formation {}", formationId);
        return inscriptionRepository.findByFormationId(formationId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public void confirmerPresence(String inscriptionId) {
        log.info("✅ Confirmation de présence pour l'inscription {}", inscriptionId);

        // 1. Récupérer l'inscription
        InscriptionFormation inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        // 2. Récupérer la formation
        Formation formation = formationRepository.findById(inscription.getFormationId())
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        // 3. Récupérer l'employé
        User employe = userRepository.findById(inscription.getEmployeId())
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        // ==================== ✅ VÉRIFICATION 1: POINTS SUFFISANTS ====================
        int coutFormation = 1000;
        int pointsActuels = pointService.getSoldePoints(inscription.getEmployeId());

        log.info("💰 Vérification points pour {}: {} points (besoin: {})",
                employe.getEmail(), pointsActuels, coutFormation);

        if (pointsActuels < coutFormation) {
            String errorMsg = String.format(
                    "❌ Points insuffisants pour confirmer la présence. Vous avez %d points, besoin de %d points.",
                    pointsActuels, coutFormation
            );
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // ==================== ✅ VÉRIFICATION 2: STATUT DÉJÀ CONFIRMÉ ====================
        if ("CONFIRME".equals(inscription.getStatut())) {
            log.warn("⚠️ Inscription {} déjà confirmée", inscriptionId);
            throw new RuntimeException("Présence déjà confirmée pour cette inscription");
        }

        // ==================== ✅ VÉRIFICATION 3: PLACES DISPONIBLES ====================
        log.info("📊 Places avant confirmation - Max: {}, Dispo: {}",
                formation.getNombrePlaces(), formation.getPlacesDisponibles());

        if (formation.getPlacesDisponibles() == null || formation.getPlacesDisponibles() <= 0) {
            throw new RuntimeException("Plus de places disponibles pour cette formation");
        }

        // ==================== ✅ VÉRIFICATION 4: DATE LIMITE (optionnel) ====================
        if (formation.getDateLimiteInscription() != null &&
                LocalDateTime.now().isAfter(formation.getDateLimiteInscription())) {
            throw new RuntimeException("Date limite d'inscription dépassée");
        }

        // ==================== ÉTAPE 1: DÉCRÉMENTER LES PLACES ====================
        formation.setPlacesDisponibles(formation.getPlacesDisponibles() - 1);
        formationRepository.save(formation);

        log.info("📊 Places après confirmation - Nouvelles places disponibles: {}",
                formation.getPlacesDisponibles());

        // ==================== ÉTAPE 2: METTRE À JOUR L'INSCRIPTION ====================
        inscription.setPresenceConfirmee(true);
        inscription.setDatePresence(LocalDateTime.now());

        if ("EN_ATTENTE".equals(inscription.getStatut())) {
            inscription.setStatut("CONFIRME");
            log.info("📝 Statut de l'inscription changé de EN_ATTENTE à CONFIRME");

            // ==================== ÉTAPE 3: DÉDUIRE LES POINTS ====================
            String employeNom = employe.getNom() + " " + employe.getPrenom();

            try {
                pointService.deduirePoints(inscription.getEmployeId(), employeNom, formation.getTitre());
                log.info("💰 Déduction de {} points pour {} (formation: {}). Nouveau solde: {}",
                        coutFormation, employeNom, formation.getTitre(), pointsActuels - coutFormation);
            } catch (Exception e) {
                log.error("❌ Erreur lors de la déduction des points: {}", e.getMessage());
                // Annuler la décrémentation des places en cas d'erreur
                formation.setPlacesDisponibles(formation.getPlacesDisponibles() + 1);
                formationRepository.save(formation);
                throw new RuntimeException("Erreur lors de la déduction des points: " + e.getMessage());
            }
        }

        // Sauvegarder l'inscription mise à jour
        inscriptionRepository.save(inscription);

        // ==================== ÉTAPE 4: FORMATER LES DATES POUR NOTIFICATION ====================
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        String dateDebutFormatted = formation.getDateDebut() != null ?
                formation.getDateDebut().format(formatter) : "Non définie";
        String dateFinFormatted = formation.getDateFin() != null ?
                formation.getDateFin().format(formatter) : "Non définie";

        // ==================== ÉTAPE 5: ENVOYER NOTIFICATION EMAIL ====================
        try {
            notificationFormationService.envoyerConfirmationPresence(
                    employe.getEmail(),
                    employe.getNom(),
                    employe.getPrenom(),
                    formation.getTitre(),
                    dateDebutFormatted,
                    dateFinFormatted,
                    formation.getFormateur(),
                    formation.getLieu(),
                    formation.getType(),
                    String.valueOf(formation.getDureeHeures()),
                    formation.getNiveau(),
                    formation.getId()
            );
            log.info("📧 Email de confirmation de présence envoyé à {}", employe.getEmail());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email: {}", e.getMessage());
            // Ne pas bloquer la confirmation si l'email échoue
        }

        // ==================== ÉTAPE 6: LOG FINAL ====================
        log.info("✅ Présence confirmée avec succès pour l'employé {} à la formation {}",
                employe.getEmail(), formation.getTitre());
    }
    @Transactional
    public void updateToTermine(String employeId) {
        log.info("Mise à jour des inscriptions en TERMINE pour l'employé {}", employeId);

        List<InscriptionFormation> inscriptions = inscriptionRepository.findByEmployeId(employeId);
        int updatedCount = 0;

        for (InscriptionFormation insc : inscriptions) {
            if ("CONFIRME".equals(insc.getStatut())) {
                insc.setStatut("TERMINE");
                inscriptionRepository.save(insc);
                updatedCount++;
                log.info("Inscription {} mise à jour en TERMINE", insc.getId());
            }
        }

        log.info("{} inscription(s) mise(s) à jour en TERMINE pour l'employé {}", updatedCount, employeId);
    }

    @Transactional
    public void setTermine(String inscriptionId) {
        log.info("Mise à jour de l'inscription {} en TERMINE", inscriptionId);

        InscriptionFormation inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        inscription.setStatut("TERMINE");
        inscriptionRepository.save(inscription);

        log.info("Inscription {} mise à jour en TERMINE", inscriptionId);
    }

    public void debugInscriptions(String employeId) {
        log.info("=== DEBUG INSCRIPTIONS ===");
        log.info("EmployeId: {}", employeId);

        List<InscriptionFormation> inscriptions = inscriptionRepository.findByEmployeId(employeId);
        log.info("Nombre d'inscriptions trouvées: {}", inscriptions.size());

        for (InscriptionFormation insc : inscriptions) {
            log.info("Inscription: {} - {}", insc.getFormationTitre(), insc.getStatut());
        }
    }

    // ==================== NOUVELLES MÉTHODES AVEC ParticipantInscription ====================
    @Transactional
    public ParticipantInscription inscrireEmployeParticipant(String formationId, String employeId) {
        log.info("Tentative d'inscription de l'employé {} à la formation {}", employeId, formationId);

        // 1. Vérifier si déjà inscrit
        if (participantRepository.existsByFormationIdAndEmployeId(formationId, employeId)) {
            throw new RuntimeException("Vous êtes déjà inscrit à cette formation");
        }

        // 2. Récupérer la formation
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        // ==================== ✅ VÉRIFICATION DU PRÉREQUIS ====================
        if (formation.getPrerequisFormationId() != null && !formation.getPrerequisFormationId().isEmpty()) {

            // Récupérer l'inscription du prérequis avec ParticipantInscription (Optional)
            Optional<ParticipantInscription> prerequisOpt = participantRepository.findByFormationIdAndEmployeId(
                    formation.getPrerequisFormationId(), employeId);

            boolean prerequisComplete = false;
            String statutPrerequis = "Non inscrit";

            if (prerequisOpt.isPresent()) {
                statutPrerequis = prerequisOpt.get().getStatut();
                prerequisComplete = "CONFIRME".equals(statutPrerequis) ||
                        "PRESENT".equals(statutPrerequis) ||
                        "TERMINE".equals(statutPrerequis);
            }

            // Vérifier aussi dans InscriptionFormation (ancien système) - retourne List
            if (!prerequisComplete) {
                List<InscriptionFormation> prerequisOldList = inscriptionRepository.findByFormationIdAndEmployeId(
                        formation.getPrerequisFormationId(), employeId);
                if (prerequisOldList != null && !prerequisOldList.isEmpty()) {
                    statutPrerequis = prerequisOldList.get(0).getStatut();
                    prerequisComplete = "CONFIRME".equals(statutPrerequis) ||
                            "PRESENT".equals(statutPrerequis) ||
                            "TERMINE".equals(statutPrerequis);
                }
            }

            if (!prerequisComplete) {
                String message = String.format(
                        "❌ Impossible de s'inscrire à \"%s\". Vous devez d'abord valider votre présence à la formation prérequise \"%s\".\nStatut actuel: %s",
                        formation.getTitre(),
                        formation.getPrerequisFormationTitre() != null ? formation.getPrerequisFormationTitre() : "Formation prérequise",
                        statutPrerequis
                );
                log.error(message);
                throw new RuntimeException(message);
            }

            log.info("✅ Prérequis validé - Statut: {}", statutPrerequis);
        }
        // ==================== FIN VÉRIFICATION PRÉREQUIS ====================

        // 3. Vérifier les places disponibles
        log.info("Places avant inscription - Max: {}, Dispo: {}",
                formation.getNombrePlaces(), formation.getPlacesDisponibles());

        if (formation.getPlacesDisponibles() == null || formation.getPlacesDisponibles() <= 0) {
            throw new RuntimeException("Plus de places disponibles pour cette formation");
        }

        // 4. Vérifier les points
        User employe = userRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        String employeNom = employe.getNom() + " " + employe.getPrenom();
        pointService.verifierEtRechargerPoints(employeId, employeNom);
        int pointsActuels = pointService.getSoldePoints(employeId);
        int coutFormation = 1000;

        if (pointsActuels < coutFormation) {
            throw new RuntimeException(String.format(
                    "Points insuffisants. Vous avez %d points, besoin de %d points.",
                    pointsActuels, coutFormation
            ));
        }

        // 5. Créer l'inscription avec statut EN_ATTENTE
        ParticipantInscription inscription = new ParticipantInscription();
        inscription.setFormationId(formationId);
        inscription.setEmployeId(employeId);
        inscription.setEmployeNom(employe.getNom());
        inscription.setEmployePrenom(employe.getPrenom());
        inscription.setEmployeEmail(employe.getEmail());
        inscription.setStatut("EN_ATTENTE");
        inscription.setPresenceValidee(false);
        inscription.setDateInscription(LocalDateTime.now());

        inscription.setFormationTitre(formation.getTitre());
        inscription.setFormateur(formation.getFormateur());
        inscription.setLieu(formation.getLieu());
        inscription.setLienVisio(formation.getLienVisio());
        inscription.setLienGoogleMaps(formation.getLienGoogleMaps());
        inscription.setDateDebut(formation.getDateDebut());
        inscription.setDateFin(formation.getDateFin());
        inscription.setDureeHeures(formation.getDureeHeures());
        inscription.setType(formation.getType());
        inscription.setNiveau(formation.getNiveau());

        ParticipantInscription savedInscription = participantRepository.save(inscription);
        log.info("✅ Inscription créée en EN_ATTENTE - ID: {}, Formation: {}", savedInscription.getId(), formation.getTitre());

        // Envoyer notification
        notificationFormationService.envoyerConfirmationInscription(employeId, formation.getTitre());

        return savedInscription;
    }
    @Transactional
    public ParticipantInscription validerPresenceParticipant(String formationId, String employeId, String commentaire) {
        log.info("✅ Validation de présence pour {} à la formation {}", employeId, formationId);

        ParticipantInscription inscription = participantRepository.findByFormationIdAndEmployeId(formationId, employeId)
                .orElseThrow(() -> new RuntimeException("Participant non inscrit à cette formation"));

        // Vérifier si déjà confirmé
        if ("CONFIRME".equals(inscription.getStatut())) {
            throw new RuntimeException("Présence déjà confirmée pour cette inscription");
        }

        // Récupérer la formation
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        // 🔥 DÉCRÉMENTER LES PLACES ICI (lors de la confirmation)
        log.info("Places avant confirmation - Max: {}, Dispo: {}",
                formation.getNombrePlaces(), formation.getPlacesDisponibles());

        if (formation.getPlacesDisponibles() <= 0) {
            throw new RuntimeException("Plus de places disponibles pour cette formation");
        }

        formation.setPlacesDisponibles(formation.getPlacesDisponibles() - 1);
        formationRepository.save(formation);

        log.info("📊 Places après confirmation - Nouvelles places disponibles: {}",
                formation.getPlacesDisponibles());

        // 🔥 DÉDUIRE LES POINTS ICI (lors de la confirmation)
        String employeNom = inscription.getEmployeNom() + " " + inscription.getEmployePrenom();
        int coutFormation = 1000;

        try {
            pointService.deduirePoints(employeId, employeNom, formation.getTitre());
            log.info("💰 Déduction de {} points pour {} (formation: {}).",
                    coutFormation, employeNom, formation.getTitre());
        } catch (Exception e) {
            log.error("Erreur lors de la déduction des points: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la déduction des points: " + e.getMessage());
        }

        // Mettre à jour l'inscription
        inscription.setPresenceValidee(true);
        inscription.setStatut("PRESENT");  // ← Passage de EN_ATTENTE à CONFIRME
        inscription.setDatePresence(LocalDateTime.now());

        if (commentaire != null && !commentaire.isEmpty()) {
            inscription.setCommentaire(commentaire);
        }

        ParticipantInscription savedInscription = participantRepository.save(inscription);

        log.info("✅ Présence confirmée - Inscription: {}, Statut: {}",
                savedInscription.getId(), savedInscription.getStatut());

        return savedInscription;
    }

    @Transactional(readOnly = true)
    public List<ParticipantInscription> getInscriptionsByEmployeParticipant(String employeId) {
        log.info("Récupération des inscriptions Participant pour l'employé {}", employeId);
        return participantRepository.findByEmployeId(employeId);
    }


    @Transactional
    public ParticipantInscription marquerAbsentParticipant(String formationId, String employeId, String commentaire) {
        log.info("❌ Marquage absent pour {} à la formation {}", employeId, formationId);

        ParticipantInscription inscription = participantRepository.findByFormationIdAndEmployeId(formationId, employeId)
                .orElseThrow(() -> new RuntimeException("Participant non inscrit à cette formation"));

        inscription.setStatut("ABSENT");
        inscription.setPresenceValidee(false);
        inscription.setCommentaire(commentaire);

        return participantRepository.save(inscription);
    }

    @Transactional
    public ParticipantInscription reinitialiserStatutParticipant(String formationId, String employeId) {
        log.info("🔄 Réinitialisation du statut pour {} à la formation {}", employeId, formationId);

        ParticipantInscription inscription = participantRepository.findByFormationIdAndEmployeId(formationId, employeId)
                .orElseThrow(() -> new RuntimeException("Participant non inscrit à cette formation"));

        inscription.setStatut("INSCRIT");
        inscription.setPresenceValidee(false);
        inscription.setDatePresence(null);
        inscription.setCommentaire(null);

        return participantRepository.save(inscription);
    }
    // Méthode modifiée pour ne pas nécessiter employeId en paramètre
    // Méthode modifiée pour ne pas nécessiter employeId en paramètre
    @Transactional
    public Map<String, Object> annulerInscription(String inscriptionId, String motif, String typeMotif) {
        log.info("📝 Annulation inscription: {}, Motif: {}, Type: {}", inscriptionId, motif, typeMotif);

        Map<String, Object> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // ==================== 1. RÉCUPÉRER L'INSCRIPTION (PARTICIPANT OU ANCIENNE) ====================
        Optional<ParticipantInscription> participantOpt = participantRepository.findById(inscriptionId);
        Optional<InscriptionFormation> inscriptionOpt = inscriptionRepository.findById(inscriptionId);

        if (participantOpt.isEmpty() && inscriptionOpt.isEmpty()) {
            throw new RuntimeException("Inscription non trouvée");
        }

        String employeId;
        String formationId;
        String formationTitre;
        String statutActuel;
        String employeEmail = "";
        String employeNom = "";
        String employePrenom = "";
        Formation formation;

        // ==================== 2. TRAITER SELON LE TYPE D'INSCRIPTION ====================
        if (participantOpt.isPresent()) {
            ParticipantInscription inscription = participantOpt.get();
            employeId = inscription.getEmployeId();
            formationId = inscription.getFormationId();
            formationTitre = inscription.getFormationTitre();
            statutActuel = inscription.getStatut();
            employeEmail = inscription.getEmployeEmail();
            employeNom = inscription.getEmployeNom();
            employePrenom = inscription.getEmployePrenom();

            formation = formationRepository.findById(formationId)
                    .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

            // Vérifier les délais
            LocalDateTime dateDebut = formation.getDateDebut();
            if (now.isAfter(dateDebut)) {
                throw new RuntimeException("La formation a déjà commencé, vous ne pouvez plus l'annuler");
            }

            // Calculer le remboursement
            long joursAvantDebut = java.time.temporal.ChronoUnit.DAYS.between(now, dateDebut);
            int pointsARembourser = 0;
            String messageRemboursement = "";

            if (joursAvantDebut >= 7) {
                pointsARembourser = 1000;
                messageRemboursement = "Remboursement intégral de 1000 points.";
            } else if (joursAvantDebut >= 3) {
                pointsARembourser = 500;
                messageRemboursement = "Remboursement partiel de 500 points.";
            } else {
                messageRemboursement = "Délai d'annulation dépassé, aucun remboursement.";
            }

            // Rembourser si applicable
            if (pointsARembourser > 0 && ("CONFIRME".equals(statutActuel) || "EN_ATTENTE".equals(statutActuel))) {
                pointService.ajouterPoints(employeId, pointsARembourser,
                        "Remboursement pour annulation de: " + formation.getTitre());
                log.info("💰 Remboursement de {} points pour {}", pointsARembourser, employeEmail);
            }

            // Mettre à jour l'inscription
            inscription.setStatut("ANNULE");
            inscription.setMotifAnnulation(motif);
            inscription.setDateAnnulation(now);
            participantRepository.save(inscription);

            // Libérer la place
            formation.setPlacesDisponibles(formation.getPlacesDisponibles() + 1);
            formationRepository.save(formation);

            // ==================== 3. ANNULATION EN CASCADE POUR ParticipantInscription ====================
            annulerCascadeParticipant(formation, employeId, motif, now);

            // Envoyer l'email
            User employe = userRepository.findById(employeId).orElse(null);
            if (employe != null) {
                notificationFormationService.envoyerEmailAnnulationAvecRemboursement(
                        employe.getEmail(), employe.getNom(), employe.getPrenom(),
                        formation.getTitre(), formatDate(formation.getDateDebut()), formatDate(formation.getDateFin()),
                        formation.getFormateur(), formation.getLieu(), formation.getType(),
                        motif, pointsARembourser, messageRemboursement, formation.getId()
                );
            } else if (!employeEmail.isEmpty()) {
                // Fallback avec les données de l'inscription
                notificationFormationService.envoyerEmailAnnulationAvecRemboursement(
                        employeEmail, employeNom, employePrenom,
                        formation.getTitre(), formatDate(formation.getDateDebut()), formatDate(formation.getDateFin()),
                        formation.getFormateur(), formation.getLieu(), formation.getType(),
                        motif, pointsARembourser, messageRemboursement, formation.getId()
                );
            }

            result.put("success", true);
            result.put("pointsRembourses", pointsARembourser);
            result.put("message", messageRemboursement);
            result.put("dateAnnulation", now);
            result.put("formationTitre", formation.getTitre());

        } else {
            // ==================== TRAITER InscriptionFormation (ancien système) ====================
            InscriptionFormation inscription = inscriptionOpt.get();
            employeId = inscription.getEmployeId();
            formationId = inscription.getFormationId();
            formationTitre = inscription.getFormationTitre();
            statutActuel = inscription.getStatut();

            formation = formationRepository.findById(formationId)
                    .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

            User employe = userRepository.findById(employeId)
                    .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

            // Vérifier les délais
            LocalDateTime dateDebut = formation.getDateDebut();
            if (now.isAfter(dateDebut)) {
                throw new RuntimeException("La formation a déjà commencé, vous ne pouvez plus l'annuler");
            }

            // Calculer le remboursement
            long joursAvantDebut = java.time.temporal.ChronoUnit.DAYS.between(now, dateDebut);
            int pointsARembourser = 0;
            String messageRemboursement = "";

            if (joursAvantDebut >= 7) {
                pointsARembourser = 1000;
                messageRemboursement = "Remboursement intégral de 1000 points.";
            } else if (joursAvantDebut >= 3) {
                pointsARembourser = 500;
                messageRemboursement = "Remboursement partiel de 500 points.";
            } else {
                messageRemboursement = "Délai d'annulation dépassé, aucun remboursement.";
            }

            if (pointsARembourser > 0 && ("CONFIRME".equals(statutActuel) || "EN_ATTENTE".equals(statutActuel))) {
                pointService.ajouterPoints(employeId, pointsARembourser,
                        "Remboursement pour annulation de: " + formation.getTitre());
            }

            inscription.setStatut("ANNULE");
            inscription.setMotifAnnulation(motif);
            inscriptionRepository.save(inscription);

            formation.setPlacesDisponibles(formation.getPlacesDisponibles() + 1);
            formationRepository.save(formation);

            // ==================== ANNULATION EN CASCADE POUR InscriptionFormation ====================
            annulerCascadeInscriptionFormation(formation, employeId, motif, now);

            notificationFormationService.envoyerAnnulationInscription(
                    employe.getEmail(), employe.getNom(), employe.getPrenom(),
                    formation.getTitre(), formatDate(formation.getDateDebut()), formatDate(formation.getDateFin()),
                    formation.getFormateur(), formation.getLieu(), formation.getType(), motif, formation.getId()
            );

            result.put("success", true);
            result.put("pointsRembourses", pointsARembourser);
            result.put("message", messageRemboursement);
            result.put("dateAnnulation", now);
            result.put("formationTitre", formation.getTitre());
        }

        return result;
    }

// ==================== MÉTHODES D'ANNULATION EN CASCADE ====================

    private void annulerCascadeParticipant(Formation formation, String employeId, String motif, LocalDateTime now) {
        List<Formation> formationsDependantes = formationRepository.findByPrerequisFormationId(formation.getId());

        for (Formation formationDependante : formationsDependantes) {
            Optional<ParticipantInscription> inscDependanteOpt = participantRepository.findByFormationIdAndEmployeId(
                    formationDependante.getId(), employeId);

            if (inscDependanteOpt.isPresent()) {
                ParticipantInscription inscDependante = inscDependanteOpt.get();
                if (!"ANNULE".equals(inscDependante.getStatut())) {

                    // Rembourser si la formation dépendante était confirmée
                    if ("CONFIRME".equals(inscDependante.getStatut())) {
                        pointService.ajouterPoints(employeId, 1000,
                                "Annulation en cascade: " + formationDependante.getTitre());
                    }

                    inscDependante.setStatut("ANNULE");
                    inscDependante.setMotifAnnulation("Annulation en cascade : la formation prérequise '" +
                            formation.getTitre() + "' a été annulée");
                    inscDependante.setDateAnnulation(now);
                    participantRepository.save(inscDependante);
                    log.info("📢 Annulation en cascade Participant: {} annulée", formationDependante.getTitre());

                    // Libérer la place
                    formationDependante.setPlacesDisponibles(formationDependante.getPlacesDisponibles() + 1);
                    formationRepository.save(formationDependante);

                    // Envoyer notification
                    User employe = userRepository.findById(employeId).orElse(null);
                    if (employe != null) {
                        notificationFormationService.envoyerAnnulationCascade(
                                employe.getEmail(), employe.getNom(), employe.getPrenom(),
                                formationDependante.getTitre(), formation.getTitre(), motif
                        );
                    } else {
                        notificationFormationService.envoyerAnnulationCascade(
                                inscDependante.getEmployeEmail(), inscDependante.getEmployeNom(), inscDependante.getEmployePrenom(),
                                formationDependante.getTitre(), formation.getTitre(), motif
                        );
                    }
                }
            }
        }
    }

    private void annulerCascadeInscriptionFormation(Formation formation, String employeId, String motif, LocalDateTime now) {
        List<Formation> formationsDependantes = formationRepository.findByPrerequisFormationId(formation.getId());

        for (Formation formationDependante : formationsDependantes) {
            List<InscriptionFormation> inscriptionsDependantes = inscriptionRepository.findByFormationIdAndEmployeId(
                    formationDependante.getId(), employeId);

            for (InscriptionFormation inscDependante : inscriptionsDependantes) {
                if (!"ANNULE".equals(inscDependante.getStatut())) {

                    if ("CONFIRME".equals(inscDependante.getStatut())) {
                        pointService.ajouterPoints(employeId, 1000,
                                "Annulation en cascade: " + formationDependante.getTitre());
                    }

                    inscDependante.setStatut("ANNULE");
                    inscDependante.setMotifAnnulation("Annulation en cascade : la formation prérequise '" +
                            formation.getTitre() + "' a été annulée");
                    inscriptionRepository.save(inscDependante);
                    log.info("📢 Annulation en cascade InscriptionFormation: {} annulée", formationDependante.getTitre());

                    formationDependante.setPlacesDisponibles(formationDependante.getPlacesDisponibles() + 1);
                    formationRepository.save(formationDependante);
                }
            }
        }
    }
    // Méthode utilitaire pour formater la date
    private String formatDate(LocalDateTime date) {
        if (date == null) return "Non définie";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        return date.format(formatter);
    }
    // ==================== MÉTHODES UTILITAIRES ====================

    private InscriptionFormationDTO convertToDTO(InscriptionFormation inscription) {
        InscriptionFormationDTO dto = new InscriptionFormationDTO();
        dto.setId(inscription.getId());
        dto.setFormationId(inscription.getFormationId());
        dto.setEmployeId(inscription.getEmployeId());
        dto.setEmployeNom(inscription.getEmployeNom());
        dto.setStatut(inscription.getStatut());
        dto.setDateInscription(inscription.getDateInscription());
        dto.setMotifAnnulation(inscription.getMotifAnnulation());
        dto.setPresenceConfirmee(inscription.isPresenceConfirmee());
        dto.setDatePresence(inscription.getDatePresence());
        dto.setFormationTitre(inscription.getFormationTitre());
        dto.setFormateur(inscription.getFormateur());
        dto.setLieu(inscription.getLieu());
        dto.setLienVisio(inscription.getLienVisio());
        dto.setLienGoogleMaps(inscription.getLienGoogleMaps());
        dto.setDateDebut(inscription.getDateDebut());
        dto.setDateFin(inscription.getDateFin());
        dto.setDureeHeures(inscription.getDureeHeures());

        return dto;
    }

}