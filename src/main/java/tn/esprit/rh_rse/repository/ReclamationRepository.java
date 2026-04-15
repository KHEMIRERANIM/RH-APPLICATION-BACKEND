package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Reclamation;

@Repository
public interface ReclamationRepository extends MongoRepository<Reclamation, String> {
}
