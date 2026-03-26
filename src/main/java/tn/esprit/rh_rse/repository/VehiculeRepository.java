package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Vehicule;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculeRepository extends MongoRepository<Vehicule, String> {
    List<Vehicule> findByEmployeId(String employeId);
    Optional<Vehicule> findByImmatriculation(String immatriculation);
}