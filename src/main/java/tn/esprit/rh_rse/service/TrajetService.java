package tn.esprit.rh_rse.service;


import tn.esprit.rh_rse.entity.Trajet;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;

import java.util.List;

public interface TrajetService {
    List<Trajet> getAll();
    Trajet getById(String id);
    Trajet create(Trajet trajet);
    Trajet update(String id, Trajet trajet);
    void delete(String id);
    void updateStatus(String id, StatutTrajet statut);
    List<Trajet> getByStatut(StatutTrajet statut);
}