package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> getMesNotifications(String idUser);
    Notification marquerCommeLu(String idNotification, String idUser);
    void marquerToutCommeLu(String idUser);
    long getNbNonLues(String idUser);
}
