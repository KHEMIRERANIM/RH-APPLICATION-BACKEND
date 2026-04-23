package tn.esprit.rh_rse.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.service.EmailService;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Override
    public void sendReservationConfirmation(String toEmail, String userName, String offreTitle,
            byte[] pdfAttachment) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Réservation confirmée : " + offreTitle);
            helper.setFrom("admin.adentreprise@gmail.com", "Service Avantages Sociaux");

            String htmlContent = buildHtmlContent(userName, offreTitle);
            helper.setText(htmlContent, true);

            if (pdfAttachment != null) {
                String safeTitle = offreTitle.replaceAll("[^a-zA-Z0-9.-]", "_");
                helper.addAttachment("Confirmation_" + safeTitle + ".pdf", new ByteArrayResource(pdfAttachment));
            }

            javaMailSender.send(message);

        } catch (Exception e) {
            log.error("Erreur envoi email réservation : {}", e.getMessage());
        }
    }

    @Override
    public void envoyerConfirmationCandidature(String emailCandidat, String nomCandidat, String titreOffre) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailCandidat);
            helper.setSubject("Votre candidature a été reçue — " + titreOffre);
            helper.setText(buildEmailCandidature(nomCandidat, titreOffre), true);
            javaMailSender.send(message);
            log.info("Email confirmation candidature envoyé à {}", emailCandidat);
        } catch (MessagingException e) {
            log.error("Erreur envoi email candidature : {}", e.getMessage());
        }
    }

    @Override
    public void envoyerConvocationEntretien(String emailCandidat, String nomCandidat, String titreOffre,
            String typeEntretien, LocalDateTime dateHeure, Integer dureeMinutes, String lienVisio, String lieu) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailCandidat);
            helper.setSubject("Convocation entretien — " + titreOffre);
            helper.setText(buildEmailConvocation(nomCandidat, titreOffre, typeEntretien, dateHeure, dureeMinutes,
                    lienVisio, lieu), true);
            javaMailSender.send(message);
            log.info("Email convocation entretien envoyé à {}", emailCandidat);
        } catch (MessagingException e) {
            log.error("Erreur envoi email convocation : {}", e.getMessage());
        }
    }

    @Override
    public void envoyerRappelRecruteur(String emailRecruteur, String nomRecruteur, String typeEntretien,
            LocalDateTime dateHeure, String lienVisio) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailRecruteur);
            helper.setSubject("Rappel entretien planifié — " + dateHeure.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
            helper.setText(buildEmailRappelRecruteur(nomRecruteur, typeEntretien, dateHeure, lienVisio), true);
            javaMailSender.send(message);
            log.info("Email rappel recruteur envoyé à {}", emailRecruteur);
        } catch (MessagingException e) {
            log.error("Erreur envoi email rappel recruteur : {}", e.getMessage());
        }
    }

    @Override
    public void envoyerChangementStatut(String emailCandidat, String nomCandidat, String titreOffre,
            String nouveauStatut) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailCandidat);
            helper.setSubject("Mise à jour de votre candidature — " + titreOffre);
            helper.setText(buildEmailStatut(nomCandidat, titreOffre, nouveauStatut), true);
            javaMailSender.send(message);
            log.info("Email changement statut envoyé à {}", emailCandidat);
        } catch (MessagingException e) {
            log.error("Erreur envoi email statut : {}", e.getMessage());
        }
    }

    @Override
    public void envoyerEmailAcceptation(String emailCandidat, String nomCandidat, String titreOffre, String societe,
            String qrCodeBase64, String lienMeet) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailCandidat);
            helper.setSubject("🎉 Félicitations ! Vous êtes accepté(e) — " + titreOffre);
            helper.setText(buildEmailAcceptation(nomCandidat, titreOffre, societe, lienMeet), true);

            if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) {
                byte[] qrBytes = Base64.getDecoder().decode(qrCodeBase64);
                ByteArrayResource resource = new ByteArrayResource(qrBytes);
                helper.addInline("qrcode", resource, "image/png");
            }

            javaMailSender.send(message);
            log.info("Email acceptation avec QR Code envoyé à {}", emailCandidat);

        } catch (MessagingException e) {
            log.error("Erreur envoi email acceptation : {}", e.getMessage());
        }
    }

    private String buildHtmlContent(String userName, String offreTitle) {
        return "<!DOCTYPE html>" +
                "<html lang=\"fr\">" +
                "<head><style>" +
                "body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; }"
                +
                ".container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }"
                +
                ".header { background: linear-gradient(135deg, #4c1d95, #6366f1); color: #ffffff; padding: 40px 30px; text-align: center; }"
                +
                ".header h1 { margin: 0; font-size: 24px; font-weight: 700; }" +
                ".content { padding: 40px 30px; color: #334155; line-height: 1.6; font-size: 16px; }" +
                ".content p { margin: 0 0 20px 0; }" +
                ".highlight { font-weight: bold; color: #4f46e5; }" +
                ".footer { background-color: #f1f5f9; padding: 20px; text-align: center; color: #64748b; font-size: 13px; border-top: 1px solid #e2e8f0; }"
                +
                "</style></head>" +
                "<body>" +
                "<div class=\"container\">" +
                "  <div class=\"header\">" +
                "    <h1>Réservation Confirmée</h1>" +
                "  </div>" +
                "  <div class=\"content\">" +
                "    <p>Bonjour <strong>" + userName + "</strong>,</p>" +
                "    <p>Nous sommes ravis de vous confirmer votre réservation pour l'offreAvantage détaillée ci-dessous :</p>" +
                "    <p class=\"highlight\">" + offreTitle + "</p>" +
                "    <p>Vous trouverez en pièce jointe votre <strong>fiche de réservation officielle</strong> au format PDF. Nous vous invitons à la conserver précieusement et à la présenter si nécessaire.</p>"
                +
                "    <p>Merci de votre confiance et à très bientôt.<br/>L'équipe Mutuelle & Avantages Sociaux.</p>" +
                "  </div>" +
                "  <div class=\"footer\">" +
                "    Cet email a été généré automatiquement, merci de ne pas y répondre.<br/>" +
                "    Entreprise © 2026 - Tous droits réservés." +
                "  </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildEmailCandidature(String nom, String titreOffre) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb;">
                  <div style="background: #4f46e5; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0;">RH Platform</h1>
                  </div>
                  <div style="padding: 30px; background: #ffffff;">
                    <h2 style="color: #1f2937;">Bonjour %s,</h2>
                    <p style="color: #4b5563;">Votre candidature pour le poste <strong>%s</strong> a bien été reçue.</p>
                    <p style="color: #4b5563;">Notre équipe RH va examiner votre dossier et vous contactera prochainement.</p>
                    <p style="color: #6b7280; border-top: 1px solid #eee;">Cordialement,<br><strong>L'équipe RH</strong></p>
                  </div>
                </div>
                """.formatted(nom, titreOffre);
    }

    private String buildEmailConvocation(
            String nom, String titreOffre, String type,
            LocalDateTime dateHeure, Integer duree,
            String lienVisio, String lieu) {

        String dateFormatee = dateHeure.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        StringBuilder details = new StringBuilder();

        if (lienVisio != null && !lienVisio.trim().isEmpty()) {
            details.append("""
                    <div style="background: #dcfce7; border-radius: 8px; padding: 15px; margin: 20px 0; border: 1px solid #bbf7d0;">
                      <p style="color: #166534; margin: 0;">
                        🎥 <strong>Lien Google Meet :</strong><br>
                        <a href="%s" style="color: #15803d; font-weight: bold;">%s</a>
                      </p>
                    </div>
                    """.formatted(lienVisio, lienVisio));
        }

        if (lieu != null && !lieu.trim().isEmpty()) {
            details.append("""
                    <div style="background: #fef9c3; border-radius: 8px; padding: 15px; margin: 20px 0;">
                      <p style="color: #854d0e; margin: 0;">📍 <strong>Lieu :</strong> %s</p>
                    </div>
                    """.formatted(lieu));
        }

        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb;">
                  <div style="background: #4f46e5; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0;">RH Platform</h1>
                  </div>
                  <div style="padding: 30px; background: #ffffff;">
                    <h2 style="color: #1f2937;">Bonjour %s,</h2>
                    <p>Vous êtes convoqué(e) à un entretien pour le poste <strong>%s</strong>.</p>
                    <div style="background: #f3f4f6; border-radius: 8px; padding: 20px; margin: 20px 0; border-left: 4px solid #4f46e5;">
                      <p style="margin: 5px 0;">📅 <strong>Date :</strong> %s</p>
                      <p style="margin: 5px 0;">⏱️ <strong>Durée :</strong> %d minutes</p>
                      <p style="margin: 5px 0;">💼 <strong>Type :</strong> %s</p>
                    </div>
                    %s
                    <p style="color: #6b7280;">Cordialement,<br><strong>L'équipe RH</strong></p>
                  </div>
                </div>
                """.formatted(nom, titreOffre, dateFormatee, duree, type, details.toString());
    }

    private String buildEmailRappelRecruteur(
            String nom, String type,
            LocalDateTime dateHeure, String lienVisio) {

        String dateFormatee = dateHeure.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        String meetSection = (lienVisio != null && !lienVisio.isEmpty()) ? """
                <div style="background: #dcfce7; border-radius: 8px; padding: 15px; margin: 20px 0;">
                  <p style="color: #166534; margin: 0;">
                    🎥 <strong>Rejoindre la visio :</strong><br>
                    <a href="%s" style="color: #15803d; font-weight: bold;">%s</a>
                  </p>
                </div>
                """.formatted(lienVisio, lienVisio) : "";

        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb;">
                  <div style="background: #111827; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0;">Rappel Entretien</h1>
                  </div>
                  <div style="padding: 30px; background: #ffffff;">
                    <h2 style="color: #1f2937;">Bonjour %s,</h2>
                    <p>Vous avez un entretien <strong>%s</strong> prévu le <strong>%s</strong>.</p>
                    %s
                    <p style="color: #6b7280;">Cordialement,<br><strong>Système RH</strong></p>
                  </div>
                </div>
                """.formatted(nom, type, dateFormatee, meetSection);
    }

    private String buildEmailStatut(String nom, String titreOffre, String statut) {
        String messageSatut = switch (statut) {
            case "ENTRETIEN_RH" -> "Félicitations ! Vous êtes convoqué(e) à un entretien RH.";
            case "ENTRETIEN_TECHNIQUE" -> "Votre dossier avance : vous passez à l'étape technique.";
            case "OFFRE_ENVOYEE" -> "🎉 Une offre d'embauche vous a été envoyée !";
            case "ACCEPTE" -> "🎊 Félicitations ! Votre candidature a été acceptée !";
            case "REFUSE" -> "Votre candidature n'a pas été retenue pour ce poste.";
            default -> "Le statut de votre candidature a changé.";
        };

        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb;">
                  <div style="padding: 30px; background: #ffffff;">
                    <h2 style="color: #1f2937;">Bonjour %s,</h2>
                    <p>Mise à jour pour le poste : <strong>%s</strong></p>
                    <div style="background: #f3f4f6; padding: 20px; border-radius: 8px; text-align: center; font-size: 18px; color: #4f46e5;">
                      %s
                    </div>
                  </div>
                </div>
                """.formatted(nom, titreOffre, messageSatut);
    }

    private String buildEmailAcceptation(
            String nomCandidat,
            String titreOffre,
            String societe,
            String lienMeet) {

        String meetSection = "";
        if (lienMeet != null && !lienMeet.isEmpty()) {
            meetSection = "<div style='background: #dcfce7; border-radius: 8px; padding: 16px; margin: 20px 0; border: 1px solid #bbf7d0;'>"
                    +
                    "<p style='color: #166534; margin: 0;'>" +
                    "&#127909; <strong>Lien Google Meet de votre entretien :</strong><br>" +
                    "<a href='" + lienMeet + "' style='color: #15803d; font-weight: bold;'>" + lienMeet + "</a>" +
                    "</p></div>";
        }

        return "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 20px;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden;'>" +

                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px; text-align: center;'>"
                +
                "<h1 style='color: white; margin: 0;'>&#127881; Félicitations !</h1>" +
                "<p style='color: rgba(255,255,255,0.9); margin: 10px 0 0;'>Votre candidature a été acceptée</p>" +
                "</div>" +

                "<div style='padding: 40px;'>" +
                "<h2 style='color: #333;'>Bonjour " + nomCandidat + ",</h2>" +
                "<p>Votre candidature pour <strong>" + titreOffre + "</strong> chez <strong>" + societe
                + "</strong> a été <strong style='color: #667eea;'>acceptée</strong> !</p>" +

                // Lien Meet
                meetSection +

                // Info charte
                "<div style='background: #f0f4ff; border-left: 4px solid #667eea; padding: 16px; border-radius: 8px; margin: 20px 0;'>"
                +
                "<strong>&#128203; Prochaine étape</strong><br>" +
                "Scannez le QR Code ci-dessous pour lire et signer la charte de notre société." +
                "</div>" +

                // QR Code
                "<div style='text-align: center; margin: 30px 0; padding: 20px; background: #fafafa; border-radius: 12px; border: 2px dashed #ddd;'>"
                +
                "<h3 style='color: #333;'>&#128241; Scannez ce QR Code</h3>" +
                "<p style='color: #666; font-size: 14px;'>Pour accéder à la charte et signer électroniquement</p>" +
                "<img src='cid:qrcode' alt='QR Code' style='width: 200px; height: 200px;'/>" +
                "</div>" +

                "<p>Bienvenue dans notre équipe ! &#128640;</p>" +
                "<p>Cordialement,<br><strong>L'équipe RH — " + societe + "</strong></p>" +
                "</div>" +

                "<div style='background: #f5f5f5; padding: 20px; text-align: center; color: #999; font-size: 12px;'>" +
                "Cet email est généré automatiquement par la plateforme RH." +
                "</div>" +
                "</div></body></html>";
    }
}
