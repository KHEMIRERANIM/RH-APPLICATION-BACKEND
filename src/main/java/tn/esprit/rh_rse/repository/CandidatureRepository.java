package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Candidature;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidatureRepository extends MongoRepository<Candidature, String> {

    List<Candidature> findByOffreId(String offreId);

    List<Candidature> findByCandidatId(String candidatId);

    Optional<Candidature> findByCandidatIdAndOffreId(String candidatId, String offreId);

    List<Candidature> findByOffreIdAndStatut(String offreId, StatutCandidature statut);

    List<Candidature> findByOffreIdOrderByScoreMatchingDesc(String offreId);

    long countByOffreId(String offreId);
}