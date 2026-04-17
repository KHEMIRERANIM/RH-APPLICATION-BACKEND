package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.entity.Career;
import tn.esprit.rh_rse.entity.EvolutionPlanEntity;

import java.util.List;
import java.util.Map;

public interface EvolutionPlanRepository extends MongoRepository<EvolutionPlanEntity, String> {

    List<EvolutionPlanEntity> findByTargetCareerId(String targetCareerId);

    List<EvolutionPlanEntity> findByEmployeeId(String employeeId);
}