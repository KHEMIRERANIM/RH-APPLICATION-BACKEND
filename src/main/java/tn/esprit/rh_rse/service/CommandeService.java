package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Commande;

import java.util.List;
import java.util.Map;

public interface CommandeService {
    List<Commande> getAll();
    Commande getById(String id);
    Commande save(Commande commande);
    Commande updateStatut(String id, String statut);
    void delete(String id);
    List<Commande> getByUser(String userId);

    // Nouveaux endpoints statistiques
    Map<String, Long> getNombreCommandesParJour();
    Map<String, Long> getPlatsPlusCommandes();
}