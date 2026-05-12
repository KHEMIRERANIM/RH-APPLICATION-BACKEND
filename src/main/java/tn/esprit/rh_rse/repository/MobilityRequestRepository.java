package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.MobilityRequest;
import tn.esprit.rh_rse.entity.enums.MobilityStatus;

import java.util.List;

public interface MobilityRequestRepository extends MongoRepository<MobilityRequest, String> {

    List<MobilityRequest> findByEmployeeId(String employeeId);

    List<MobilityRequest> findByStatus(MobilityStatus status);

    List<MobilityRequest> findByTargetCareerId(String targetCareerId);

    boolean existsByEmployeeIdAndTargetCareerIdAndStatus(
            String employeeId,
            String targetCareerId,
            MobilityStatus status
    );

}