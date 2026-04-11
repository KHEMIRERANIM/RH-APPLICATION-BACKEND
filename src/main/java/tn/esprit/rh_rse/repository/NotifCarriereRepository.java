package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.NotifCarriere;
import java.util.List;

public interface NotifCarriereRepository extends MongoRepository<NotifCarriere, String> {
    List<NotifCarriere> findByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserIdAndReadFalse(String userId);
}