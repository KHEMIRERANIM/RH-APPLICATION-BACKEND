package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.enums.CategorieOffre;

import java.util.List;

public interface OffreService {
    Offre creerOffre(Offre offre);
    Offre modifierOffre(String id, Offre offre);
    void supprimerOffre(String id);
    Offre getById(String id);
    List<Offre> getAllActives();
    List<Offre> getByCategorie(CategorieOffre categorie);
    List<Offre> getByPartenaire(String idPartenaire);
    Offre toggleStatut(String id);
}