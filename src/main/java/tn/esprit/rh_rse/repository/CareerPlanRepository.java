package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.CareerPlan;
import tn.esprit.rh_rse.entity.enums.PlanStatus;

import java.util.List;
import java.util.Optional;

public interface CareerPlanRepository extends MongoRepository<CareerPlan, String> {
    List<CareerPlan> findByEmployeeId(String employeeId);
    List<CareerPlan> findByStatus(PlanStatus status);
    Optional<CareerPlan> findByEmployeeIdAndTargetCareerId(String employeeId, String targetCareerId);
}