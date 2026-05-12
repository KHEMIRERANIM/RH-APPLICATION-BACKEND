package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.RseAction;
import tn.esprit.rh_rse.entity.User;

import java.util.List;

public interface RseActionRepository extends MongoRepository<RseAction, String> {

    List<RseAction> findByEmployee(User user);

    List<RseAction> findByStatus(String status);

    List<RseAction> findByEmployeeAndStatus(User user, String status);

    long countByEmployeeAndStatus(User user, String status);

    long countByEmployeeAndTypeAndStatus(User user, String type, String status);

    List<RseAction> findByEmployee_Id(String employeeId);

    List<RseAction> findByEmployee_IdOrderByCreatedAtDesc(String employeeId);

    List<RseAction> findByStatusOrderByCreatedAtDesc(String status);
}