package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Partenaire;

import java.util.List;

public interface PartenaireService {
    Partenaire creerPartenaire(Partenaire partenaire);
    Partenaire modifierPartenaire(String id, Partenaire partenaire);
    void supprimerPartenaire(String id);
    Partenaire getById(String id);
    List<Partenaire> getAll();
    Partenaire toggleActif(String id);
}