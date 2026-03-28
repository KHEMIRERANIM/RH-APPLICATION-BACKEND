package tn.esprit.rh_rse.service;

public interface EmailService {
    void sendReservationConfirmation(String toEmail, String userName, String offreTitle, byte[] pdfAttachment);
}
