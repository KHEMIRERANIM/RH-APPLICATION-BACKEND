package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.NotificationTransport;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.util.List;

public interface NotificationTransportService {
    List<NotificationTransport> getAll();
    NotificationTransport getById(String id);
    NotificationTransport create(NotificationTransport notification);
    void delete(String id);
    List<NotificationTransport> getByDestinataire(String destinataireId);
    List<NotificationTransport> getNonLues(String destinataireId);
    NotificationTransport marquerCommeLu(String id);
    void marquerToutesCommeLues(String destinataireId);
    long countNonLues(String destinataireId);
    NotificationTransport envoyerNotification(String destinataireId, String expediteurId,
                                     String trajetId, TypeNotification type,
                                     String contenu);
    NotificationTransport envoyerDemandeConfirmation(String conducteurId, String passagerId,
                                            String trajetId, String reservationId);
}
