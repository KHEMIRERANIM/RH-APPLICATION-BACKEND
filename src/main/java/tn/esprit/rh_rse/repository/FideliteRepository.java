package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Fidelite;
import java.util.Optional;

public interface FideliteRepository extends MongoRepository<Fidelite, String> {
    Optional<Fidelite> findByUserId(String userId);
}
