package tn.esprit.rh_rse.repository.Formation;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.InscriptionFormation;

import java.util.List;

@Repository
public interface InscriptionFormationRepository extends MongoRepository<InscriptionFormation, String> {

    List<InscriptionFormation> findByEmployeId(String employeId);

    List<InscriptionFormation> findByFormationId(String formationId);

    @Query(value = "{ 'formationId': ?0, 'statut': 'CONFIRME' }", count = true)
    int countInscriptionsConfirmees(String formationId);

    boolean existsByFormationIdAndEmployeId(String formationId, String employeId);

    List<InscriptionFormation> findByFormationIdAndStatut(String formationId, String statut);
}