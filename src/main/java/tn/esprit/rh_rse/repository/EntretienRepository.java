package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Entretien;

import java.util.List;

@Repository
public interface EntretienRepository extends MongoRepository<Entretien, String> {

    List<Entretien> findByCandidatureId(String candidatureId);

    List<Entretien> findByRecruteurId(String recruteurId);

    List<Entretien> findByCandidatureIdIn(List<String> candidatureIds);
}