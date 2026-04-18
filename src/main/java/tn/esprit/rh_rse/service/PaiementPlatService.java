package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.PaiementPlat;
import java.util.List;

public interface PaiementPlatService {
    PaiementPlat payerCommande(String commandeId, String modePaiement);
    List<PaiementPlat> getPaiementsByUser(String userId);
    List<PaiementPlat> getPaiementsEnAttente();
}