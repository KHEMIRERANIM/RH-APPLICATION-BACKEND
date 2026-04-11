package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Fidelite;

public interface FideliteService {
    Fidelite getOrCreate(String userId);
    Fidelite ajouterPoints(String userId, double montant);
    Fidelite utiliserReduction(String userId);
}
