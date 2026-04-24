package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.Formateur;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormateurRepository extends MongoRepository<Formateur, String> {
    Optional<Formateur> findByEmail(String email);
    Optional<Formateur> findByUserId(String userId);
    List<Formateur> findByStatus(String status);
    List<Formateur> findBySpecialiteContainingIgnoreCase(String specialite);
    List<Formateur> findByFormationsAssigneesContaining(String formationId);
}