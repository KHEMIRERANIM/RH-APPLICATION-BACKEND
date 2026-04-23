package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.SignatureCharte;

import java.util.Optional;

@Repository
public interface SignatureCharteRepository extends MongoRepository<SignatureCharte, String> {
    Optional<SignatureCharte> findByCandidatureId(String candidatureId);
    boolean existsByCandidatureId(String candidatureId);
}