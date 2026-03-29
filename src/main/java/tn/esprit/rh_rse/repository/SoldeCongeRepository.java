package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.SoldeConge;

import java.util.Optional;

@Repository
public interface SoldeCongeRepository extends MongoRepository<SoldeConge, String> {

    Optional<SoldeConge> findByEmployeIdAndAnnee(String employeId, int annee);
}
