package tn.esprit.rh_rse.service.conge;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    public void envoyerEmailValidation(String employeId, String statut, String commentaire,
                                       LocalDate dateDebut, LocalDate dateFin) {
        User employe = userRepository.findById(employeId).orElse(null);
        if (employe == null) return;

        String sujet = statut.equals("APPROUVE") ?
                "✅ Demande de congé approuvée" :
                "❌ Demande de congé refusée";

        String contenu = String.format(
                "<html>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
                        "<h2 style='color: %s;'>%s</h2>" +
                        "<p>Bonjour <strong>%s %s</strong>,</p>" +
                        "<p>Votre demande de congé du <strong>%s</strong> au <strong>%s</strong> a été <strong>%s</strong>.</p>" +
                        "%s" +
                        "<hr>" +
                        "<p style='font-size: 12px; color: #888;'>Espace RH - Application de gestion des congés</p>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                statut.equals("APPROUVE") ? "#4CAF50" : "#F44336",
                sujet,
                employe.getPrenom(), employe.getNom(),
                dateDebut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dateFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                statut.equals("APPROUVE") ? "approuvée" : "refusée",
                commentaire != null && !commentaire.isEmpty() ?
                        "<p><strong>Commentaire :</strong> " + commentaire + "</p>" : ""
        );

        envoyer(employe.getEmail(), sujet, contenu);
    }

    private void envoyer(String destinataire, String sujet, String contenuHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(contenuHtml, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
        }
    }
}