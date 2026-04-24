package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.AvisFormation;

import java.util.List;

public interface AvisService {
    List<AvisFormation> getAll();
    AvisFormation getById(String id);
    AvisFormation save(AvisFormation avisFormation);
    void delete(String id);
    List<AvisFormation> getByPlat(String platId);
    double getMoyenneNote(String platId);
}