package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.EmpreinteCarbone;

import java.util.List;

public interface EmpreinteCarboneService {
    List<EmpreinteCarbone> getAll();
    EmpreinteCarbone getById(String id);
    EmpreinteCarbone create(EmpreinteCarbone empreinte);
    EmpreinteCarbone update(String id, EmpreinteCarbone empreinte);
    void delete(String id);
    List<EmpreinteCarbone> getByEmployeId(String employeId);
    List<EmpreinteCarbone> getByTrajetId(String trajetId);
    int getTotalPointsByEmploye(String employeId);
}