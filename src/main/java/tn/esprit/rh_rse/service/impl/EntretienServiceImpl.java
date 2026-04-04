package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.CreateEntretienRequest;
import tn.esprit.rh_rse.dto.request.FeedbackEntretienRequest;
import tn.esprit.rh_rse.dto.response.EntretienResponse;
import tn.esprit.rh_rse.entity.Candidature;
import tn.esprit.rh_rse.entity.Entretien;
import tn.esprit.rh_rse.entity.enums.StatutEntretien;
import tn.esprit.rh_rse.entity.enums.TypeEntretien;
import tn.esprit.rh_rse.exception.RecrutementNotFoundException;
import tn.esprit.rh_rse.repository.CandidatureRepository;
import tn.esprit.rh_rse.repository.EntretienRepository;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.EmailService;
import tn.esprit.rh_rse.service.EntretienService;
import tn.esprit.rh_rse.service.GoogleMeetService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntretienServiceImpl implements EntretienService {

    private final EntretienRepository entretienRepository;
    private final CandidatureRepository candidatureRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final GoogleMeetService googleMeetService;
    private final OffreRepository offreRepository;

    @Override
    public EntretienResponse planifierEntretien(CreateEntretienRequest request) {

        // 1. Vérification de la candidature
        Candidature candidature = candidatureRepository
                .findById(request.getCandidatureId())
                .orElseThrow(() -> new RecrutementNotFoundException(
                        "Candidature introuvable : " + request.getCandidatureId()));

        // 2. Génération du lien Meet si type VISIO
        String lienVisio = request.getLienVisio();

        if (TypeEntretien.VISIO.equals(request.getType())) {
            log.info("Type VISIO détecté — génération Meet...");
            try {
                String emailRecruteur = userRepository
                        .findById(request.getRecruteurId())
                        .map(u -> u.getEmail())
                        .orElse("recruteur@entreprise.tn");

                String emailCandidat = userRepository
                        .findById(candidature.getCandidatId())
                        .map(u -> u.getEmail())
                        .orElse("candidat@email.com");

                String meetLink = googleMeetService.creerMeetEtObtenirLien(
                        "Entretien RH",
                        request.getDateHeure(),
                        request.getDureeMinutes(),
                        emailRecruteur,
                        emailCandidat
                );

                if (meetLink != null) {
                    lienVisio = meetLink;
                    log.info("✅ Meet créé : {}", meetLink);
                } else {
                    log.warn("⚠️ Meet link est null");
                }

            } catch (Exception e) {
                log.error("❌ Erreur création Meet : {}", e.getMessage());
            }
        }

        // 3. Construction et sauvegarde de l'entretien
        Entretien entretien = Entretien.builder()
                .candidatureId(request.getCandidatureId())
                .recruteurId(request.getRecruteurId())
                .type(request.getType())
                .dateHeure(request.getDateHeure())
                .dureeMinutes(request.getDureeMinutes())
                .lieu(request.getLieu())
                .lienVisio(lienVisio)  // ← Lien Meet généré
                .statut(StatutEntretien.PLANIFIE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Entretien saved = entretienRepository.save(entretien);
        log.info("Entretien sauvegardé — lienVisio: {}", saved.getLienVisio());

        // 4. Récupère le titre de l'offre
        String titreOffre = offreRepository
                .findById(candidature.getOffreId())
                .map(o -> o.getTitre())
                .orElse("Poste de Recrutement");

        // 5. Envoi email convocation au candidat
        try {
            userRepository.findById(candidature.getCandidatId()).ifPresent(candidat -> {
                emailService.envoyerConvocationEntretien(
                        candidat.getEmail(),
                        candidat.getPrenom() + " " + candidat.getNom(),
                        titreOffre,
                        request.getType().name(),
                        request.getDateHeure(),
                        request.getDureeMinutes(),
                        saved.getLienVisio(),  // ← Lien Meet dans l'email
                        request.getLieu()
                );
                log.info("✅ Email convocation envoyé à {}", candidat.getEmail());
            });
        } catch (Exception e) {
            log.warn("⚠️ Erreur email candidat : {}", e.getMessage());
        }

        // 6. Envoi email rappel au recruteur
        try {
            userRepository.findById(request.getRecruteurId()).ifPresent(recruteur -> {
                emailService.envoyerRappelRecruteur(
                        recruteur.getEmail(),
                        recruteur.getPrenom() + " " + recruteur.getNom(),
                        request.getType().name(),
                        request.getDateHeure(),
                        saved.getLienVisio()  // ← Lien Meet dans l'email recruteur
                );
                log.info("✅ Email rappel envoyé au recruteur {}", recruteur.getEmail());
            });
        } catch (Exception e) {
            log.warn("⚠️ Erreur email recruteur : {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    public EntretienResponse getEntretienById(String id) {
        return toResponse(entretienRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException(
                        "Entretien introuvable : " + id)));
    }

    @Override
    public List<EntretienResponse> getEntretiensParCandidature(String candidatureId) {
        return entretienRepository.findByCandidatureId(candidatureId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<EntretienResponse> getEntretiensParRecruteur(String recruteurId) {
        return entretienRepository.findByRecruteurId(recruteurId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public EntretienResponse modifierEntretien(String id, CreateEntretienRequest request) {
        Entretien e = entretienRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException(
                        "Entretien introuvable : " + id));

        if (request.getType() != null)         e.setType(request.getType());
        if (request.getDateHeure() != null)    e.setDateHeure(request.getDateHeure());
        if (request.getDureeMinutes() != null) e.setDureeMinutes(request.getDureeMinutes());
        if (request.getLieu() != null)         e.setLieu(request.getLieu());

        // Si VISIO et pas de lien → génère nouveau Meet
        if (TypeEntretien.VISIO.equals(request.getType())
                && (request.getLienVisio() == null || request.getLienVisio().isEmpty())) {
            try {
                Candidature candidature = candidatureRepository
                        .findById(e.getCandidatureId()).orElse(null);

                if (candidature != null) {
                    String emailRecruteur = userRepository
                            .findById(e.getRecruteurId())
                            .map(u -> u.getEmail())
                            .orElse("recruteur@entreprise.tn");
                    String emailCandidat = userRepository
                            .findById(candidature.getCandidatId())
                            .map(u -> u.getEmail())
                            .orElse("candidat@email.com");

                    String meetLink = googleMeetService.creerMeetEtObtenirLien(
                            "Entretien RH",
                            request.getDateHeure() != null ? request.getDateHeure() : e.getDateHeure(),
                            request.getDureeMinutes() != null ? request.getDureeMinutes() : e.getDureeMinutes(),
                            emailRecruteur,
                            emailCandidat
                    );

                    if (meetLink != null) {
                        e.setLienVisio(meetLink);
                        log.info("Meet mis à jour : {}", meetLink);
                    }
                }
            } catch (Exception ex) {
                log.warn("Erreur Meet modification : {}", ex.getMessage());
            }
        } else if (request.getLienVisio() != null) {
            e.setLienVisio(request.getLienVisio());
        }

        e.setUpdatedAt(LocalDateTime.now());
        return toResponse(entretienRepository.save(e));
    }

    @Override
    public EntretienResponse ajouterFeedback(String id, FeedbackEntretienRequest request) {
        Entretien e = entretienRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException(
                        "Entretien introuvable : " + id));

        e.setFeedbackGlobal(request.getFeedbackGlobal());
        e.setNoteGlobale(request.getNoteGlobale());
        e.setPointsForts(request.getPointsForts());
        e.setPointsFaibles(request.getPointsFaibles());
        e.setRecommandeEmbauche(request.isRecommandeEmbauche());
        e.setStatut(StatutEntretien.REALISE);
        e.setUpdatedAt(LocalDateTime.now());

        return toResponse(entretienRepository.save(e));
    }

    @Override
    public void annulerEntretien(String id) {
        Entretien e = entretienRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException(
                        "Entretien introuvable : " + id));
        e.setStatut(StatutEntretien.ANNULE);
        e.setUpdatedAt(LocalDateTime.now());
        entretienRepository.save(e);
    }

    @Override
    public void marquerRealise(String id) {
        Entretien e = entretienRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException(
                        "Entretien introuvable : " + id));
        e.setStatut(StatutEntretien.REALISE);
        e.setUpdatedAt(LocalDateTime.now());
        entretienRepository.save(e);
    }

    private EntretienResponse toResponse(Entretien e) {
        return EntretienResponse.builder()
                .id(e.getId())
                .candidatureId(e.getCandidatureId())
                .recruteurId(e.getRecruteurId())
                .type(e.getType())
                .dateHeure(e.getDateHeure())
                .dureeMinutes(e.getDureeMinutes())
                .lieu(e.getLieu())
                .lienVisio(e.getLienVisio())
                .statut(e.getStatut())
                .feedbackGlobal(e.getFeedbackGlobal())
                .noteGlobale(e.getNoteGlobale())
                .pointsForts(e.getPointsForts())
                .pointsFaibles(e.getPointsFaibles())
                .recommandeEmbauche(e.isRecommandeEmbauche())
                .createdAt(e.getCreatedAt())
                .build();
    }
}