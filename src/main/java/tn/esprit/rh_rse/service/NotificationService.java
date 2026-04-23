package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.util.List;

public interface NotificationService {
    // HEAD (Mutuelle)
    List<Notification> getMesNotifications(String idUser);
    Notification marquerCommeLu(String idNotification, String idUser);
    void marquerToutCommeLu(String idUser);
    long getNbNonLues(String idUser);
    void supprimerNotification(String idNotification, String idUser);

    // transport
    List<Notification> getAll();
    Notification getById(String id);
    Notification create(Notification notification);
    void delete(String id);
    List<Notification> getByDestinataire(String destinataireId);
    List<Notification> getNonLues(String destinataireId);
    Notification marquerCommeLu(String id);
    void marquerToutesCommeLues(String destinataireId);
    long countNonLues(String destinataireId);
    Notification envoyerNotification(String destinataireId, String expediteurId,
                                     String trajetId, TypeNotification type,
                                     String contenu);
    Notification envoyerDemandeConfirmation(String conducteurId, String passagerId,
                                            String trajetId, String reservationId);
}
