package tn.esprit.rh_rse.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByDestinataireId(String destinataireId);
    List<Notification> findByDestinataireIdAndLu(String destinataireId, Boolean lu);
    List<Notification> findByDestinataireIdAndType(String destinataireId, TypeNotification type);
    List<Notification> findByTrajetId(String trajetId);
    long countByDestinataireIdAndLu(String destinataireId, Boolean lu);
}