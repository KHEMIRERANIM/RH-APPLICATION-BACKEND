package tn.esprit.rh_rse.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.service.EmailService;

@Service
@RequiredArgsConstructor
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
            System.err.println("Erreur ignorée lors de l'envoi de l'email : " + e.getMessage());
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
}
