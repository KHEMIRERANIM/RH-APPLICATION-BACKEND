package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.OffreAvantage;
import tn.esprit.rh_rse.entity.enums.CategorieOffreAvantage;

import java.util.List;

public interface OffreAvantageService {
    OffreAvantage creerOffreAvantage(OffreAvantage offreAvantage);
    OffreAvantage modifierOffreAvantage(String id, OffreAvantage offreAvantage);
    void supprimerOffreAvantage(String id);
    OffreAvantage getById(String id);
    List<OffreAvantage> getAllActives();
    List<OffreAvantage> getByCategorie(CategorieOffreAvantage categorie);
    List<OffreAvantage> getByPartenaire(String idPartenaire);
    OffreAvantage toggleStatut(String id);
}