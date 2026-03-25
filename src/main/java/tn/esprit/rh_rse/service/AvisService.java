package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Avis;

import java.util.List;

public interface AvisService {
    List<Avis> getAll();
    Avis getById(String id);
    Avis save(Avis avis);
    void delete(String id);
    List<Avis> getByPlat(String platId);
    double getMoyenneNote(String platId);
}