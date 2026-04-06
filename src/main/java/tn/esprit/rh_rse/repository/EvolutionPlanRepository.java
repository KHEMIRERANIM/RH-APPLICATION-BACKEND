package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Career;
import tn.esprit.rh_rse.entity.EvolutionPlanEntity;

import java.util.List;

public interface EvolutionPlanRepository extends MongoRepository<EvolutionPlanEntity, String> {

    List<EvolutionPlanEntity> findByTargetCareerId(String targetCareerId);

    List<EvolutionPlanEntity> findByEmployeeId(String employeeId);

}