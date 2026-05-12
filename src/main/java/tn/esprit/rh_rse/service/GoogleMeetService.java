package tn.esprit.rh_rse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class GoogleMeetService {

    public String creerMeetEtObtenirLien(
            String titre,
            LocalDateTime dateHeure,
            Integer dureeMinutes,
            String emailRecruteur,
            String emailCandidat) {
        try {
            // Format : EntretienRH-DATE-CODEALEATOIRE
            String date = dateHeure.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
            String code = UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase();

            String roomName = "EntretienRH-" + date + "-" + code;
            String lien = "https://meet.jit.si/" + roomName;

            log.info("✅ Lien Jitsi Meet généré : {}", lien);
            log.info("   Recruteur : {}", emailRecruteur);
            log.info("   Candidat  : {}", emailCandidat);

            return lien;
        } catch (Exception e) {
            log.error("❌ Erreur génération lien Jitsi : {}", e.getMessage());
            return null;
        }
    }
}