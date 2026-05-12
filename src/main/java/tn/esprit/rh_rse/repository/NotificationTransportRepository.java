package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.NotificationTransport;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.util.List;

@Repository
public interface NotificationTransportRepository extends MongoRepository<NotificationTransport, String> {
    List<NotificationTransport> findByDestinataireId(String destinataireId);
    List<NotificationTransport> findByDestinataireIdAndLu(String destinataireId, Boolean lu);
    List<NotificationTransport> findByDestinataireIdAndType(String destinataireId, TypeNotification type);
    List<NotificationTransport> findByTrajetId(String trajetId);
    long countByDestinataireIdAndLu(String destinataireId, Boolean lu);
}
