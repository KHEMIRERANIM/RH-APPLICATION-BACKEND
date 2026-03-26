package tn.esprit.rh_rse.service.Formation;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public void envoyerConfirmationInscription(String employeId, String formationTitre) {
        // À implémenter avec votre système de notification
        System.out.println("Notification envoyée à l'employé " + employeId +
                " pour confirmation inscription à " + formationTitre);
    }

    public void envoyerRappelFormation(String employeId, String formationTitre, LocalDateTime dateDebut) {
        System.out.println("Rappel envoyé à l'employé " + employeId +
                " pour formation " + formationTitre + " le " + dateDebut);
    }
}