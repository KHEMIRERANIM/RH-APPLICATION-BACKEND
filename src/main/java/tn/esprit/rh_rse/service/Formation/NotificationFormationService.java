package tn.esprit.rh_rse.service.Formation;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationFormationService {

    private final JavaMailSender mailSender;
    private final QrCodeFormationService qrCodeFormationService;
    private final JavaMailSender javaMailSender;  // ✅ Injection du mail sender


    public void envoyerConfirmationInscription(String employeId, String formationTitre) {
        log.info(" Notification d'inscription pour l'employé {} à la formation {}", employeId, formationTitre);
    }

    public void envoyerRappelFormation(String employeId, String formationTitre, LocalDateTime dateDebut) {
        log.info(" Rappel pour {} - Formation: {} le {}", employeId, formationTitre, dateDebut);
    }
    public void envoyerAnnulationCascade(String email, String nom, String prenom,
                                         String formationTitre, String prerequisTitre,
                                         String motif) {
        String sujet = "Annulation de votre formation en cascade";
        String message = String.format(
                "Bonjour %s %s,\n\n" +
                        "Votre inscription à la formation \"%s\" a été automatiquement annulée.\n\n" +
                        "Motif : La formation prérequise \"%s\" a été annulée.\n\n" +
                        "Motif d'annulation du prérequis : %s\n\n" +
                        "Cordialement,\n" +
                        "L'équipe RH & RSE",
                prenom, nom, formationTitre, prerequisTitre, motif
        );

        envoyerEmail(email, sujet, message);
    }


    public void envoyerEmail(String destinataire, String sujet, String corpsHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(corpsHtml, true);
            helper.setFrom("rh-rse@entreprise.tn");

            mailSender.send(message);
            log.info(" Email envoyé avec succès à {}", destinataire);
        } catch (Exception e) {
            log.error(" Erreur lors de l'envoi de l'email à {}: {}", destinataire, e.getMessage());
        }
    }

    public void envoyerConfirmationPresence(String destinataire, String nomEmploye, String prenomEmploye,
                                            String formationTitre, String dateDebut, String dateFin,
                                            String formateur, String lieu, String type,
                                            String dureeHeures, String niveau, String formationId) {
        String sujet = "Confirmation de presence - " + formationTitre;

        // Generer le QR Code
        String qrCodeBase64 = qrCodeFormationService.genererQRCodeFormation(formationId, formationTitre, "");

        String qrCodeHtml = "";
        if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) {
            qrCodeHtml = String.format("""
                <div style="text-align: center; margin: 25px 0; padding: 20px; background: #f8fafc; border-radius: 16px;">
                    <div style="font-size: 14px; font-weight: 600; color: #4a5568; margin-bottom: 15px;">Votre QR Code d'acces</div>
                    <div style="background: white; padding: 15px; border-radius: 12px; display: inline-block;">
                        <img src="data:image/png;base64,%s" style="width: 180px; height: 180px;" alt="QR Code">
                    </div>
                    <p style="font-size: 12px; color: #718096; margin-top: 12px;">Scannez ce code pour acceder aux informations de la formation</p>
                </div>
                """, qrCodeBase64);
        }

        String corpsHtml = String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <title>Confirmation de presence - %s</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; background-color: #f0f2f5; padding: 20px; }
                    .email-container { max-width: 600px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 35px -10px rgba(0,0,0,0.1); }
                    .email-header { background: linear-gradient(135deg, #10b981, #059669); padding: 30px; text-align: center; }
                    .email-header h1 { color: white; font-size: 28px; margin-bottom: 10px; }
                    .email-header p { color: rgba(255,255,255,0.9); }
                    .content { padding: 30px; }
                    .greeting { font-size: 18px; color: #2d3748; margin-bottom: 20px; }
                    .greeting strong { color: #10b981; }
                    .training-card { background: #f8fafc; border-radius: 16px; padding: 20px; margin: 20px 0; border-left: 4px solid #10b981; }
                    .training-title { font-size: 20px; font-weight: bold; color: #10b981; margin-bottom: 15px; }
                    .training-details { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
                    .detail-item { display: flex; align-items: center; gap: 10px; padding: 8px; background: white; border-radius: 10px; }
                    .detail-icon { font-size: 20px; }
                    .detail-label { font-size: 11px; color: #718096; }
                    .detail-value { font-weight: 600; color: #2d3748; }
                    .info-box { background: #ecfdf5; border-radius: 16px; padding: 20px; margin: 20px 0; }
                    .info-box h3 { color: #059669; margin-bottom: 12px; }
                    .info-box ul { list-style: none; padding-left: 0; }
                    .info-box li { padding: 6px 0; display: flex; align-items: center; gap: 10px; }
                    .info-box li:before { content: "✓"; color: #10b981; font-weight: bold; }
                    .cta-button { display: inline-block; background: linear-gradient(135deg, #10b981, #059669); color: white; padding: 12px 28px; text-decoration: none; border-radius: 12px; font-weight: 600; margin-top: 20px; }
                    .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #718096; border-top: 1px solid #e2e8f0; }
                    @media (max-width: 600px) { .training-details { grid-template-columns: 1fr; } }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="email-header">
                        <h1>Confirmation de presence</h1>
                        <p>Votre inscription est validee</p>
                    </div>
                    <div class="content">
                        <div class="greeting">Bonjour <strong>%s %s</strong>,</div>
                        <p>Nous vous confirmons votre presence a la formation :</p>
                        <div class="training-card">
                            <div class="training-title">%s</div>
                            <div class="training-details">
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Date</div><div class="detail-value">%s - %s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Duree</div><div class="detail-value">%s heures</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Formateur</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Lieu</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Type</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Niveau</div><div class="detail-value">%s</div></div>
                                </div>
                            </div>
                        </div>
                        %s
                        <div class="info-box">
                            <h3>Informations importantes</h3>
                            <ul>
                                <li>Merci d'arriver 15 minutes avant le debut de la formation</li>
                                <li>N'oubliez pas votre badge d'acces ou une piece d'identite</li>
                                <li>Un support de formation numerique vous sera fourni</li>
                                <li>Prevoyez de quoi prendre des notes</li>
                            </ul>
                        </div>
                        <div style="text-align: center;">
                            <a href="http://localhost:4200/employee/mes-inscriptions" class="cta-button">Voir mes formations</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>RH & RSE Platform - Gestion des formations</p>
                        <p>Cet email est une confirmation automatique. Merci de ne pas y repondre.</p>
                        <p>© %d - Tous droits reserves</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                formationTitre,
                prenomEmploye,
                nomEmploye,
                formationTitre,
                dateDebut,
                dateFin,
                dureeHeures != null ? dureeHeures : "0",
                formateur != null ? formateur : "A definir",
                lieu != null ? lieu : "En ligne",
                type != null ? type : "Formation",
                niveau != null ? niveau : "Debutant",
                qrCodeHtml,
                java.time.Year.now().getValue()
        );

        envoyerEmail(destinataire, sujet, corpsHtml);
        log.info(" Email de confirmation de presence envoye a {} avec QR Code", destinataire);
    }

    public void envoyerAnnulationInscription(String destinataire, String nomEmploye, String prenomEmploye,
                                             String formationTitre, String dateDebut, String dateFin,
                                             String formateur, String lieu, String type, String motif, String formationId) {
        String sujet = "Annulation d'inscription - " + formationTitre;

        // Generer le QR Code pour l'annulation (optionnel)
        String qrCodeBase64 = qrCodeFormationService.genererQRCodeFormation(formationId, formationTitre, "");

        String qrCodeHtml = "";
        if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) {
            qrCodeHtml = String.format("""
                <div style="text-align: center; margin: 25px 0; padding: 20px; background: #fef3c7; border-radius: 16px;">
                    <div style="font-size: 14px; font-weight: 600; color: #b45309; margin-bottom: 15px;">Information sur la formation</div>
                    <div style="background: white; padding: 15px; border-radius: 12px; display: inline-block;">
                        <img src="data:image/png;base64,%s" style="width: 150px; height: 150px;" alt="QR Code">
                    </div>
                    <p style="font-size: 12px; color: #92400e; margin-top: 12px;">Scannez ce code pour revoir les details de la formation</p>
                </div>
                """, qrCodeBase64);
        }

        String corpsHtml = String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <title>Annulation d'inscription - %s</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #ef4444, #dc2626); padding: 30px; text-align: center; color: white; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .content { padding: 30px; }
                    .training-card { background: #fef2f2; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #ef4444; }
                    .motif-box { background: #fef3c7; padding: 15px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #f59e0b; }
                    .btn { display: inline-block; background: #10b981; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                    .btn-secondary { background: #6b7280; }
                    .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #6b7280; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Annulation d'inscription</h1>
                        <p>Votre inscription a ete annulee</p>
                    </div>
                    <div class="content">
                        <p>Bonjour <strong>%s %s</strong>,</p>
                        <p>Nous vous informons que votre inscription a la formation suivante a ete annulee :</p>
                        <div class="training-card">
                            <h2 style="color: #ef4444; margin: 0 0 10px 0;">%s</h2>
                            <p><strong>Date :</strong> %s - %s</p>
                            <p><strong>Formateur :</strong> %s</p>
                            <p><strong>Lieu :</strong> %s</p>
                            <p><strong>Type :</strong> %s</p>
                        </div>
                        %s
                        <div class="motif-box">
                            <h3>Motif d'annulation</h3>
                            <p style="margin: 10px 0 0 0;"><em>« %s »</em></p>
                        </div>
                        <p>La place liberee est maintenant disponible pour d'autres participants.</p>
                        <p>Si vous souhaitez vous reinscrire ou decouvrir d'autres formations, n'hesitez pas a consulter notre catalogue.</p>
                        <div style="text-align: center;">
                            <a href="http://localhost:4200/employee/formations" class="btn">Decouvrir les formations</a>
                            &nbsp;&nbsp;
                            <a href="http://localhost:4200/employee/mes-inscriptions" class="btn btn-secondary">Mes inscriptions</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>RH & RSE Platform - Cet email est une confirmation automatique</p>
                        <p>© %d - Tous droits reserves</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                formationTitre,
                prenomEmploye,
                nomEmploye,
                formationTitre,
                dateDebut,
                dateFin,
                formateur != null ? formateur : "A definir",
                lieu != null ? lieu : "En ligne",
                type != null ? type : "Formation",
                qrCodeHtml,
                motif != null ? motif : "Non specifie",
                java.time.Year.now().getValue()
        );

        envoyerEmail(destinataire, sujet, corpsHtml);
        log.info(" Email d'annulation envoye a {} pour la formation {} avec QR Code", destinataire, formationTitre);
    }

    // Dans NotificationService.java
    public void envoyerEmailSuppressionFormation(User employe, Formation formation, boolean avecRemboursement) {
        try {
            String sujet = "⚠️ Annulation de votre formation - " + formation.getTitre();

            String corpsHtml = String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <title>Annulation de formation - %s</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; background-color: #f0f2f5; padding: 20px; }
                    .email-container { max-width: 600px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 35px -10px rgba(0,0,0,0.1); }
                    .email-header { background: linear-gradient(135deg, #ef4444, #dc2626); padding: 30px; text-align: center; }
                    .email-header h1 { color: white; font-size: 28px; margin-bottom: 10px; }
                    .email-header p { color: rgba(255,255,255,0.9); }
                    .content { padding: 30px; }
                    .greeting { font-size: 18px; color: #2d3748; margin-bottom: 20px; }
                    .greeting strong { color: #dc2626; }
                    .alert-box { background: #fef2f2; border-radius: 16px; padding: 20px; margin: 20px 0; border-left: 4px solid #ef4444; }
                    .training-card { background: #f8fafc; border-radius: 16px; padding: 20px; margin: 20px 0; }
                    .training-title { font-size: 20px; font-weight: bold; color: #dc2626; margin-bottom: 15px; text-align: center; }
                    .training-details { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
                    .detail-item { display: flex; align-items: center; gap: 10px; padding: 8px; background: white; border-radius: 10px; }
                    .detail-icon { font-size: 20px; width: 30px; text-align: center; }
                    .detail-label { font-size: 11px; color: #718096; }
                    .detail-value { font-weight: 600; color: #2d3748; }
                    .refund-box { background: #ecfdf5; border-radius: 16px; padding: 20px; margin: 20px 0; text-align: center; border-left: 4px solid #10b981; }
                    .refund-box h3 { color: #059669; margin-bottom: 12px; }
                    .refund-amount { font-size: 28px; font-weight: bold; color: #10b981; margin: 10px 0; }
                    .info-box { background: #fef3c7; border-radius: 16px; padding: 20px; margin: 20px 0; }
                    .info-box h3 { color: #b45309; margin-bottom: 12px; }
                    .info-box ul { list-style: none; padding-left: 0; }
                    .info-box li { padding: 6px 0; display: flex; align-items: center; gap: 10px; }
                    .info-box li:before { content: "⚠️"; color: #f59e0b; font-weight: bold; }
                    .cta-button { display: inline-block; background: linear-gradient(135deg, #10b981, #059669); color: white; padding: 12px 28px; text-decoration: none; border-radius: 12px; font-weight: 600; margin-top: 20px; }
                    .cta-button-secondary { background: linear-gradient(135deg, #6b7280, #4b5563); margin-left: 10px; }
                    .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #718096; border-top: 1px solid #e2e8f0; }
                    @media (max-width: 600px) { .training-details { grid-template-columns: 1fr; } }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="email-header">
                        <h1>❌ Formation Annulée</h1>
                        <p>Nous sommes désolés pour ce désagrément</p>
                    </div>
                    <div class="content">
                        <div class="greeting">
                            Bonjour <strong>%s %s</strong>,
                        </div>
                        <div class="alert-box">
                            <p style="margin: 0; text-align: center;">Nous vous informons que la formation suivante a été <strong>annulée</strong> par l'administrateur :</p>
                        </div>
                        
                        <div class="training-card">
                            <div class="training-title">📚 %s</div>
                            <div class="training-details">
                                <div class="detail-item">
                                    <div class="detail-icon">👨‍🏫</div>
                                    <div><div class="detail-label">Formateur</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon">📅</div>
                                    <div><div class="detail-label">Date début</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon">⏰</div>
                                    <div><div class="detail-label">Date fin</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon">📍</div>
                                    <div><div class="detail-label">Lieu</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon">📖</div>
                                    <div><div class="detail-label">Type</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon">🎯</div>
                                    <div><div class="detail-label">Niveau</div><div class="detail-value">%s</div></div>
                                </div>
                            </div>
                        </div>
                        
                        %s
                        
                        <div class="info-box">
                            <h3>📢 Que faire maintenant ?</h3>
                            <ul>
                                <li>Consultez notre catalogue pour découvrir d'autres formations disponibles</li>
                                <li>Vous pouvez vous inscrire à une nouvelle formation avec vos points</li>
                                <li>Contactez-nous si vous avez des questions</li>
                            </ul>
                        </div>
                        
                        <div style="text-align: center;">
                            <a href="http://localhost:4200/employee/formations" class="cta-button">📖 Découvrir les formations</a>
                            <a href="http://localhost:4200/employee/mes-inscriptions" class="cta-button cta-button-secondary">📋 Mes inscriptions</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p><strong>RH & RSE Platform</strong> - Gestion des formations</p>
                        <p>Cet email est une notification automatique. Merci de ne pas y répondre.</p>
                        <p>© %d - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                    formation.getTitre(),
                    employe.getPrenom(), employe.getNom(),
                    formation.getTitre(),
                    formation.getFormateur() != null ? formation.getFormateur() : "Non spécifié",
                    formatDate(formation.getDateDebut()),
                    formatDate(formation.getDateFin()),
                    formation.getLieu() != null ? formation.getLieu() : "En ligne",
                    formation.getType() != null ? formation.getType() : "Formation",
                    formation.getNiveau() != null ? formation.getNiveau() : "Débutant",
                    avecRemboursement ? String.format("""
                <div class="refund-box">
                    <h3>✅ Remboursement effectué</h3>
                    <div class="refund-amount">1000 points</div>
                    <p>Vos points ont été remboursés et sont disponibles sur votre compte.</p>
                    <p style="margin-top: 10px; font-size: 14px;">Vous pouvez les utiliser pour vous inscrire à une nouvelle formation.</p>
                </div>
                """, formation.getTitre()) : """
                <div class="refund-box" style="background: #fef3c7; border-left-color: #f59e0b;">
                    <h3>ℹ️ Aucun remboursement</h3>
                    <p>Votre inscription n'était pas confirmée, aucun point n'a été débité.</p>
                </div>
                """,
                    java.time.Year.now().getValue()
            );

            envoyerEmail(employe.getEmail(), sujet, corpsHtml);
            log.info("📧 Email de suppression personnalisé envoyé à {}", employe.getEmail());

        } catch (Exception e) {
            log.error("❌ Erreur envoi email suppression à {}: {}", employe.getEmail(), e.getMessage());
        }
    }
    public void envoyerEmailSuppressionFormationParticipant(User employe, Formation formation, boolean avecRemboursement) {
        try {
            String sujet = "⚠️ Annulation de votre inscription - " + formation.getTitre();

            String corpsHtml = String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <title>Annulation d'inscription - %s</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; background-color: #f0f2f5; padding: 20px; }
                    .email-container { max-width: 600px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 35px -10px rgba(0,0,0,0.1); }
                    .email-header { background: linear-gradient(135deg, #ef4444, #dc2626); padding: 30px; text-align: center; }
                    .email-header h1 { color: white; font-size: 28px; margin-bottom: 10px; }
                    .email-header p { color: rgba(255,255,255,0.9); }
                    .content { padding: 30px; }
                    .greeting { font-size: 18px; color: #2d3748; margin-bottom: 20px; }
                    .training-card { background: #f8fafc; border-radius: 16px; padding: 20px; margin: 20px 0; }
                    .training-title { font-size: 20px; font-weight: bold; color: #dc2626; margin-bottom: 15px; text-align: center; }
                    .training-details { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
                    .detail-item { display: flex; align-items: center; gap: 10px; padding: 8px; background: white; border-radius: 10px; }
                    .detail-icon { font-size: 20px; width: 30px; text-align: center; }
                    .detail-label { font-size: 11px; color: #718096; }
                    .detail-value { font-weight: 600; color: #2d3748; }
                    .refund-box { background: #ecfdf5; border-radius: 16px; padding: 20px; margin: 20px 0; text-align: center; border-left: 4px solid #10b981; }
                    .cta-button { display: inline-block; background: linear-gradient(135deg, #10b981, #059669); color: white; padding: 12px 28px; text-decoration: none; border-radius: 12px; font-weight: 600; margin-top: 20px; }
                    .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #718096; border-top: 1px solid #e2e8f0; }
                    @media (max-width: 600px) { .training-details { grid-template-columns: 1fr; } }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="email-header">
                        <h1>❌ Inscription Annulée</h1>
                        <p>Votre inscription a été annulée</p>
                    </div>
                    <div class="content">
                        <div class="greeting">
                            Bonjour <strong>%s %s</strong>,
                        </div>
                        <p>Nous vous informons que votre inscription à la formation suivante a été <strong>annulée</strong> :</p>
                        
                        <div class="training-card">
                            <div class="training-title">%s</div>
                            <div class="training-details">
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Formateur</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Date début</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Date fin</div><div class="detail-value">%s</div></div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-icon"></div>
                                    <div><div class="detail-label">Lieu</div><div class="detail-value">%s</div></div>
                                </div>
                            </div>
                        </div>
                        
                        %s
                        
                        <div style="text-align: center;">
                            <a href="http://localhost:4200/employee/formations" class="cta-button">📖 Voir les formations disponibles</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p><strong>RH & RSE Platform</strong> - Gestion des formations</p>
                        <p>© %d - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                    formation.getTitre(),
                    employe.getPrenom(), employe.getNom(),
                    formation.getTitre(),
                    formation.getFormateur() != null ? formation.getFormateur() : "Non spécifié",
                    formatDate(formation.getDateDebut()),
                    formatDate(formation.getDateFin()),
                    formation.getLieu() != null ? formation.getLieu() : "En ligne",
                    avecRemboursement ? """
                <div class="refund-box">
                    <h3>✅ Remboursement effectué</h3>
                    <p style="font-size: 24px; font-weight: bold; color: #10b981; margin: 10px 0;">1000 points</p>
                    <p>Vos points ont été remboursés sur votre compte.</p>
                </div>
                """ : """
                <div class="refund-box" style="background: #fef3c7; border-left-color: #f59e0b;">
                    <h3>ℹ️ Information</h3>
                    <p>Aucun point n'a été débité car votre inscription n'était pas confirmée.</p>
                </div>
                """,
                    java.time.Year.now().getValue()
            );

            envoyerEmail(employe.getEmail(), sujet, corpsHtml);
            log.info("📧 Email de suppression participant personnalisé envoyé à {}", employe.getEmail());

        } catch (Exception e) {
            log.error("❌ Erreur envoi email suppression participant à {}: {}", employe.getEmail(), e.getMessage());
        }
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "Non définie";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        return date.format(formatter);
    }
    // NotificationService.java - Ajouter cette méthode
    public void envoyerEmailAvecPieceJointe(String to, String sujet, String body, byte[] attachment, String fileName) {
        try {
            MimeMessage message;
            message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(sujet);
            helper.setText(body, true);

            // ✅ Ajouter la pièce jointe
            helper.addAttachment(fileName, new ByteArrayResource(attachment));

            javaMailSender.send(message);
            log.info("📧 Email avec pièce jointe envoyé à {}", to);

        } catch (MessagingException e) {
            log.error("❌ Erreur envoi email: {}", e.getMessage());
        }
    }

    /**
     * Envoie un email d'annulation d'inscription avec détails du remboursement
     */
    public void envoyerEmailAnnulationAvecRemboursement(
            String destinataire,
            String nomEmploye,
            String prenomEmploye,
            String formationTitre,
            String dateDebut,
            String dateFin,
            String formateur,
            String lieu,
            String type,
            String motif,
            int pointsRembourses,
            String messageRemboursement,
            String formationId) {

        String sujet = "❌ Annulation de votre inscription - " + formationTitre;

        // Déterminer la couleur et l'icône selon le remboursement
        String remboursementColor;
        String remboursementIcon;
        String remboursementDetail;

        if (pointsRembourses >= 1000) {
            remboursementColor = "#10b981";
            remboursementIcon = "✅";
            remboursementDetail = "Remboursement intégral";
        } else if (pointsRembourses >= 500) {
            remboursementColor = "#f59e0b";
            remboursementIcon = "⚠️";
            remboursementDetail = "Remboursement partiel (50%)";
        } else {
            remboursementColor = "#ef4444";
            remboursementIcon = "❌";
            remboursementDetail = "Aucun remboursement";
        }

        String corpsHtml = String.format("""
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <title>Annulation d'inscription - %s</title>
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; background-color: #f0f2f5; padding: 20px; margin: 0; }
                .email-container { max-width: 600px; margin: 0 auto; background: white; border-radius: 24px; overflow: hidden; box-shadow: 0 20px 35px -10px rgba(0,0,0,0.15); }
                .email-header { background: linear-gradient(135deg, #ef4444, #dc2626); padding: 30px; text-align: center; }
                .email-header h1 { color: white; font-size: 28px; margin: 0 0 8px 0; }
                .email-header p { color: rgba(255,255,255,0.9); margin: 0; font-size: 14px; }
                .content { padding: 30px; }
                .greeting { font-size: 18px; color: #1e293b; margin-bottom: 20px; }
                .greeting strong { color: #dc2626; }
                .alert-box { background: #fef2f2; border-radius: 16px; padding: 20px; margin: 20px 0; border-left: 4px solid #ef4444; }
                .training-card { background: #f8fafc; border-radius: 20px; padding: 24px; margin: 20px 0; }
                .training-title { font-size: 20px; font-weight: bold; color: #dc2626; margin-bottom: 20px; text-align: center; border-bottom: 2px dashed #e2e8f0; padding-bottom: 12px; }
                .training-details { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
                .detail-item { display: flex; align-items: center; gap: 12px; padding: 12px; background: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
                .detail-icon { font-size: 24px; width: 40px; text-align: center; }
                .detail-label { font-size: 11px; color: #64748b; text-transform: uppercase; font-weight: 600; letter-spacing: 0.5px; }
                .detail-value { font-weight: 700; color: #1e293b; font-size: 14px; margin-top: 2px; }
                .refund-box { background: #ecfdf5; border-radius: 20px; padding: 24px; margin: 20px 0; text-align: center; border: 1px solid #a7f3d0; }
                .refund-box h3 { color: #059669; margin: 0 0 12px 0; font-size: 18px; }
                .refund-amount { font-size: 36px; font-weight: bold; color: #10b981; margin: 15px 0; }
                .refund-message { font-size: 14px; color: #065f46; margin-top: 10px; }
                .motif-box { background: #fffbeb; border-radius: 16px; padding: 20px; margin: 20px 0; border-left: 4px solid #f59e0b; }
                .motif-box h3 { color: #b45309; margin: 0 0 10px 0; font-size: 14px; }
                .motif-text { font-style: italic; color: #78350f; margin: 0; line-height: 1.5; }
                .info-box { background: #eff6ff; border-radius: 16px; padding: 20px; margin: 20px 0; }
                .info-box h3 { color: #1d4ed8; margin: 0 0 12px 0; font-size: 14px; }
                .info-box ul { margin: 0; padding-left: 20px; }
                .info-box li { padding: 6px 0; color: #1e40af; font-size: 13px; }
                .cta-button { display: inline-block; background: linear-gradient(135deg, #10b981, #059669); color: white; padding: 12px 28px; text-decoration: none; border-radius: 40px; font-weight: 600; margin-top: 20px; transition: transform 0.2s, box-shadow 0.2s; }
                .cta-button:hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(16,185,129,0.3); }
                .cta-button-secondary { background: linear-gradient(135deg, #6b7280, #4b5563); margin-left: 10px; }
                .footer { background: #f8fafc; padding: 24px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; }
                .footer p { margin: 5px 0; }
                @media (max-width: 600px) { .training-details { grid-template-columns: 1fr; } .content { padding: 20px; } }
            </style>
        </head>
        <body>
            <div class="email-container">
                <div class="email-header">
                    <h1>❌ Annulation d'inscription</h1>
                    <p>Votre inscription a été annulée</p>
                </div>
                <div class="content">
                    <div class="greeting">
                        Bonjour <strong>%s %s</strong>,
                    </div>
                    
                    <div class="alert-box">
                        <p style="margin: 0; text-align: center;">Nous vous informons que votre inscription à la formation suivante a été <strong>annulée</strong> :</p>
                    </div>
                    
                    <div class="training-card">
                        <div class="training-title">📚 %s</div>
                        <div class="training-details">
                            <div class="detail-item">
                                <div class="detail-icon">👨‍🏫</div>
                                <div><div class="detail-label">Formateur</div><div class="detail-value">%s</div></div>
                            </div>
                            <div class="detail-item">
                                <div class="detail-icon">📅</div>
                                <div><div class="detail-label">Date début</div><div class="detail-value">%s</div></div>
                            </div>
                            <div class="detail-item">
                                <div class="detail-icon">⏰</div>
                                <div><div class="detail-label">Date fin</div><div class="detail-value">%s</div></div>
                            </div>
                            <div class="detail-item">
                                <div class="detail-icon">📍</div>
                                <div><div class="detail-label">Lieu</div><div class="detail-value">%s</div></div>
                            </div>
                            <div class="detail-item">
                                <div class="detail-icon">📖</div>
                                <div><div class="detail-label">Type</div><div class="detail-value">%s</div></div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="refund-box" style="border-left-color: %s;">
                        <h3>%s %s</h3>
                        <div class="refund-amount" style="color: %s;">%s points</div>
                        <div class="refund-message">%s</div>
                    </div>
                    
                    <div class="motif-box">
                        <h3>📝 Motif de l'annulation</h3>
                        <p class="motif-text">« %s »</p>
                    </div>
                    
                    <div class="info-box">
                        <h3>💡 Que faire maintenant ?</h3>
                        <ul>
                            <li>Consultez notre catalogue pour découvrir d'autres formations</li>
                            <li>Vous pouvez vous réinscrire à cette formation si des places sont disponibles</li>
                            <li>Contactez le service RH pour toute question</li>
                        </ul>
                    </div>
                    
                    <div style="text-align: center;">
                        <a href="http://localhost:4200/employee/formations" class="cta-button">📖 Découvrir les formations</a>
                        <a href="http://localhost:4200/employee/mes-inscriptions" class="cta-button cta-button-secondary">📋 Mes inscriptions</a>
                    </div>
                </div>
                <div class="footer">
                    <p><strong>RH & RSE Platform</strong> - Gestion des formations</p>
                    <p>Cet email est une confirmation automatique. Merci de ne pas y répondre.</p>
                    <p>© %d - Tous droits réservés</p>
                </div>
            </div>
        </body>
        </html>
        """,
                formationTitre,
                prenomEmploye, nomEmploye,
                formationTitre,
                formateur != null ? formateur : "Non spécifié",
                dateDebut,
                dateFin,
                lieu != null ? lieu : "En ligne",
                type != null ? type : "Formation",
                remboursementColor,
                remboursementIcon, remboursementDetail,
                remboursementColor,
                pointsRembourses,
                messageRemboursement,
                motif != null ? motif : "Non spécifié",
                java.time.Year.now().getValue()
        );

        envoyerEmail(destinataire, sujet, corpsHtml);
        log.info("📧 Email d'annulation avec détails de remboursement envoyé à {} - Points remboursés: {}", destinataire, pointsRembourses);
    }
}