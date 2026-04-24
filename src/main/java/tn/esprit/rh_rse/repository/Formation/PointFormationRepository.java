package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.PointFormation;

import java.util.Optional;

@Repository
public interface PointFormationRepository extends MongoRepository<PointFormation, String> {
    Optional<PointFormation> findByEmployeId(String employeId);
}