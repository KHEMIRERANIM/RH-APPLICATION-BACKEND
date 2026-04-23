package tn.esprit.rh_rse.service;

import java.time.LocalDateTime;

public interface EmailService {
    void sendReservationConfirmation(String toEmail, String userName, String offreTitle, byte[] pdfAttachment);

    void envoyerConfirmationCandidature(String emailCandidat, String nomCandidat, String titreOffre);

    void envoyerConvocationEntretien(String emailCandidat, String nomCandidat, String titreOffre,
            String typeEntretien, LocalDateTime dateHeure, Integer dureeMinutes, String lienVisio, String lieu);

    void envoyerRappelRecruteur(String emailRecruteur, String nomRecruteur, String typeEntretien,
            LocalDateTime dateHeure, String lienVisio);

    void envoyerChangementStatut(String emailCandidat, String nomCandidat, String titreOffre, String nouveauStatut);

    void envoyerEmailAcceptation(String emailCandidat, String nomCandidat, String titreOffre, String societe,
            String qrCodeBase64, String lienMeet);
}
