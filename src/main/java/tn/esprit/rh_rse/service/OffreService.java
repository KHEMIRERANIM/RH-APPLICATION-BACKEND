package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.dto.request.CreateOffreRequest;
import tn.esprit.rh_rse.dto.request.UpdateOffreRequest;
import tn.esprit.rh_rse.dto.response.OffreResponse;
import tn.esprit.rh_rse.entity.enums.TypeContrat;

import java.util.List;

public interface OffreService {
    OffreResponse createOffre(CreateOffreRequest request, String createurId);
    List<OffreResponse> getAllOffresPubliees();
    List<OffreResponse> getOffresFiltered(String departement, TypeContrat typeContrat, String localisation, String search);
    OffreResponse getOffreById(String id);
    OffreResponse updateOffre(String id, UpdateOffreRequest request);
    void publierOffre(String id);
    void cloturerOffre(String id);
    void archiverOffre(String id);
    void deleteOffre(String id);
    List<OffreResponse> getOffresByCreateurId(String createurId);
}