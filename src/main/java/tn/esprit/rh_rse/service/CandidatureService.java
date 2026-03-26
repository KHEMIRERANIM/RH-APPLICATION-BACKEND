package tn.esprit.rh_rse.service;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.ChangerStatutRequest;
import tn.esprit.rh_rse.dto.response.CandidatureResponse;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;

import java.util.List;
import java.util.Map;

public interface CandidatureService {
    CandidatureResponse postuler(String candidatId, String offreId, MultipartFile cv, MultipartFile lettre);
    List<CandidatureResponse> getCandidaturesParOffre(String offreId);
    List<CandidatureResponse> getMesCandidatures(String candidatId);
    CandidatureResponse getCandidatureById(String id);
    CandidatureResponse changerStatut(String id, ChangerStatutRequest request);
    CandidatureResponse ajouterNotesRecruteur(String id, String notes);
    Map<StatutCandidature, List<CandidatureResponse>> getKanban(String offreId);
    List<CandidatureResponse> getTopCandidatsByScore(String offreId);
    void deleteCandidature(String id);
}