package tn.esprit.rh_rse.repository.Formation;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.EvaluationFormation;

import java.util.List;

@Repository
public interface EvaluationFormationRepository extends MongoRepository<EvaluationFormation, String> {

    List<EvaluationFormation> findByFormationId(String formationId);

    List<EvaluationFormation> findByEmployeId(String employeId);

    @Query(value = "{ 'formationId': ?0 }", fields = "{ 'note': 1 }")
    List<EvaluationFormation> findNotesByFormationId(String formationId);

    boolean existsByFormationIdAndEmployeId(String formationId, String employeId);
}