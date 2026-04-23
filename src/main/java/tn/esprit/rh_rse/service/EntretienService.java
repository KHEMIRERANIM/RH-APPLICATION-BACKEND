package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.dto.request.CreateEntretienRequest;
import tn.esprit.rh_rse.dto.request.FeedbackEntretienRequest;
import tn.esprit.rh_rse.dto.response.EntretienResponse;

import java.util.List;

public interface EntretienService {
    EntretienResponse planifierEntretien(CreateEntretienRequest request);
    EntretienResponse getEntretienById(String id);
    List<EntretienResponse> getEntretiensParCandidature(String candidatureId);
    List<EntretienResponse> getEntretiensParRecruteur(String recruteurId);
    List<EntretienResponse> getEntretiensParCandidat(String candidatId, boolean confirmedOnly);
    EntretienResponse modifierEntretien(String id, CreateEntretienRequest request);
    EntretienResponse ajouterFeedback(String id, FeedbackEntretienRequest request);
    void annulerEntretien(String id);
    void marquerRealise(String id);
    EntretienResponse confirmerPresenceCandidat(String entretienId);
}