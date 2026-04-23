package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.dto.request.BusPackRequest;
import tn.esprit.rh_rse.entity.Bus;

import java.util.List;

public interface BusService {
    List<Bus> getAll();
    Bus getById(String id);
    Bus create(Bus bus);
    /** Crée un pack : plusieurs bus partagent le même packId (actifs + inactifs). */
    List<Bus> createPack(BusPackRequest request);
    Bus update(String id, Bus bus);
    void delete(String id);
    Bus activateForDay(String busId, String date);
}