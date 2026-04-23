package tn.esprit.rh_rse.service;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.DemandeCongeRequest;
import tn.esprit.rh_rse.dto.request.ValidationCongeRequest;
import tn.esprit.rh_rse.dto.response.DemandeCongeResponse;
import tn.esprit.rh_rse.dto.response.SoldeCongeResponse;

import java.util.List;
import java.util.Map;

public interface CongeService {

    // ── EMPLOYE ──────────────────────────────────────────
    DemandeCongeResponse soumettreDemande(DemandeCongeRequest request);
    List<DemandeCongeResponse> getMesDemandes(String employeId);
    DemandeCongeResponse getDemandeById(String id);
    void annulerDemande(String id);
    SoldeCongeResponse getSoldeConge(String employeId);
    List<DemandeCongeResponse> getAllDemandesEnAttente();

    // ── MANAGER ──────────────────────────────────────────
    List<DemandeCongeResponse> getDemandesEnAttente(String managerId);
    List<DemandeCongeResponse> getToutesDemandesEquipe(String managerId);
    DemandeCongeResponse validerDemande(String id, ValidationCongeRequest request);

    // ── ALERTES TENDANCES ─────────────────────────────────
    Map<String, Object> detecterTendances(String managerId);

    // Ajoutez cette méthode
    List<DemandeCongeResponse> getAllDemandes();

    void supprimerDemande(String id);
    // ── EMPLOYE ──────────────────────────────────────────
    DemandeCongeResponse modifierDemande(String id, DemandeCongeRequest request);

    // ── EMPLOYE ──────────────────────────────────────────
    DemandeCongeResponse soumettreDemandeWithFile(DemandeCongeRequest request, MultipartFile document);
}
