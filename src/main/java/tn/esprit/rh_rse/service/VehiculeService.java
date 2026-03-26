package tn.esprit.rh_rse.service;


import tn.esprit.rh_rse.entity.Vehicule;

import java.util.List;

public interface VehiculeService {
    List<Vehicule> getAll();
    Vehicule getById(String id);
    Vehicule create(Vehicule vehicule);
    Vehicule update(String id, Vehicule vehicule);
    void delete(String id);
}