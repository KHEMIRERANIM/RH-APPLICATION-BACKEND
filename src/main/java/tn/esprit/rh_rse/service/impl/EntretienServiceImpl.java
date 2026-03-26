package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.CreateEntretienRequest;
import tn.esprit.rh_rse.dto.request.FeedbackEntretienRequest;
import tn.esprit.rh_rse.dto.response.EntretienResponse;
import tn.esprit.rh_rse.entity.Entretien;
import tn.esprit.rh_rse.entity.enums.StatutEntretien;
import tn.esprit.rh_rse.exception.RecrutementNotFoundException;
import tn.esprit.rh_rse.repository.CandidatureRepository;
import tn.esprit.rh_rse.repository.EntretienRepository;
import tn.esprit.rh_rse.service.EntretienService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntretienServiceImpl implements EntretienService {

    private final EntretienRepository entretienRepository;
    private final CandidatureRepository candidatureRepository;

    @Override
    public EntretienResponse planifierEntretien(CreateEntretienRequest request) {
        candidatureRepository.findById(request.getCandidatureId())
                .orElseThrow(() -> new RecrutementNotFoundException(
                        "Candidature introuvable : " + request.getCandidatureId()));

        Entretien entretien = Entretien.builder()
                .candidatureId(request.getCandidatureId())
                .recruteurId(request.getRecruteurId())
                .type(request.getType())
                .dateHeure(request.getDateHeure())
                .dureeMinutes(request.getDureeMinutes())
                .lieu(request.getLieu())
                .lienVisio(request.getLienVisio())
                .statut(StatutEntretien.PLANIFIE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(entretienRepository.save(entretien));
    }

    @Override
    public EntretienResponse getEntretienById(String id) {
        return toResponse(entretienRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Entretien introuvable : " + id)));
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
                .orElseThrow(() -> new RecrutementNotFoundException("Entretien introuvable : " + id));
        if (request.getType() != null)         e.setType(request.getType());
        if (request.getDateHeure() != null)    e.setDateHeure(request.getDateHeure());
        if (request.getDureeMinutes() != null) e.setDureeMinutes(request.getDureeMinutes());
        if (request.getLieu() != null)         e.setLieu(request.getLieu());
        if (request.getLienVisio() != null)    e.setLienVisio(request.getLienVisio());
        e.setUpdatedAt(LocalDateTime.now());
        return toResponse(entretienRepository.save(e));
    }

    @Override
    public EntretienResponse ajouterFeedback(String id, FeedbackEntretienRequest request) {
        Entretien e = entretienRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Entretien introuvable : " + id));
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
                .orElseThrow(() -> new RecrutementNotFoundException("Entretien introuvable : " + id));
        e.setStatut(StatutEntretien.ANNULE);
        e.setUpdatedAt(LocalDateTime.now());
        entretienRepository.save(e);
    }

    @Override
    public void marquerRealise(String id) {
        Entretien e = entretienRepository.findById(id)
                .orElseThrow(() -> new RecrutementNotFoundException("Entretien introuvable : " + id));
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