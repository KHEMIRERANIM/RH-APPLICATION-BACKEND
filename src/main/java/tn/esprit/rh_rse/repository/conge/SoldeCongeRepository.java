package tn.esprit.rh_rse.repository.conge;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.conge.SoldeConge;

import java.util.Optional;

@Repository
public interface SoldeCongeRepository extends MongoRepository<SoldeConge, String> {

    Optional<SoldeConge> findByEmployeIdAndAnnee(String employeId, int annee);
}
