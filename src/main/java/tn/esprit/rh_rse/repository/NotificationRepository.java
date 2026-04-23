package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByIdUserOrderByDateCreationDesc(String idUser);
    long countByIdUserAndLuFalse(String idUser);
}
