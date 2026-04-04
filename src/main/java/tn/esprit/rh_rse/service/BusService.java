package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Bus;
import java.util.List;

public interface BusService {
    List<Bus> getAll();
    Bus getById(String id);
    Bus create(Bus bus);
    Bus update(String id, Bus bus);
    void delete(String id);
}