package tn.esprit.rh_rse.service.Formation;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.entity.Formation.Examen;
import tn.esprit.rh_rse.entity.Formation.ResultatExamen;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.Formation.ExamenRepository;
import tn.esprit.rh_rse.repository.Formation.ResultatExamenRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificationService {

    private final ResultatExamenRepository resultatRepository;
    private final ExamenRepository examenRepository;
    private final UserRepository userRepository;
    private final NotificationFormationService notificationFormationService;
    private final QrCodeFormationService qrCodeFormationService;

    @Value("${certification.storage.path:uploads/certifications}")
    private String storagePath;

    private static final double SEUIL_REUSSITE = 10.0;

    @Transactional
    public void genererCertification(String formationId, String employeId, ResultatExamen resultat) {
        log.info("🎓 Vérification certification pour employé {} - Formation {}", employeId, formationId);

        if (resultat.getNote() < SEUIL_REUSSITE) {
            log.info("❌ Note insuffisante ({}/20) - Pas de certification", resultat.getNote());
            return;
        }

        log.info("✅ Note suffisante ({}/20) - Génération du PDF", resultat.getNote());

        Examen examen = examenRepository.findById(resultat.getExamenId()).orElse(null);
        User employe = userRepository.findById(employeId).orElse(null);

        if (examen == null || employe == null) {
            log.error("❌ Impossible de générer la certification: données manquantes");
            return;
        }

        String certificationId = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String dateCertification = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));

        // Générer le QR Code pour la certification
        Map<String, Object> qrData = new HashMap<>();
        qrData.put("certificationId", certificationId);
        qrData.put("employeNom", employe.getNom());
        qrData.put("employePrenom", employe.getPrenom());
        qrData.put("formationTitre", examen.getTitre());
        qrData.put("note", resultat.getNote());
        qrData.put("date", dateCertification);

        String qrCodeBase64 = qrCodeFormationService.genererQRCodeFormation(qrData);

        // Générer le PDF avec logo et QR Code
        byte[] pdfBytes = genererPDFCertification(employe, examen, resultat.getNote(), certificationId, dateCertification, qrCodeBase64);

        // Sauvegarder le PDF
        String pdfPath = sauvegarderPDF(pdfBytes, certificationId, employeId);

        log.info("🎓 PDF généré: {} pour {} {} - Note: {}/20",
                pdfPath, employe.getPrenom(), employe.getNom(), resultat.getNote());

        // Envoyer l'email avec le PDF en pièce jointe
        envoyerEmailAvecPDF(employe, examen, resultat.getNote(), certificationId, dateCertification, pdfBytes);
    }

    public byte[] genererPDFCertification(User employe, Examen examen, double note, String certificationId, String dateCertification, String qrCodeBase64) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // ==================== AJOUT DU LOGO À DROITE (comme le QR code) ====================
            try {
                // Chemin du logo
                String logoPath = "src/main/resources/Images-Formation/656937894_1603370367617311_4683203010572948516_n.png";
                File logoFile = new File(logoPath);

                if (logoFile.exists()) {
                    Image logo = Image.getInstance(logoFile.getAbsolutePath());
                    logo.scaleToFit(80, 80);
                    // ✅ Logo à droite (x=450, y=750) comme le QR code
                    logo.setAbsolutePosition(450, 750);
                    document.add(logo);
                    log.info("✅ Logo image chargé avec succès à droite");
                } else {
                    log.warn("⚠️ Logo non trouvé, utilisation du texte alternatif à droite");
                    // Fallback: logo texte à droite
                    Font logoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new BaseColor(79, 70, 229));
                    Paragraph logoText = new Paragraph("🎓 RH Formation", logoFont);
                    logoText.setAlignment(Element.ALIGN_RIGHT);
                    logoText.setSpacingAfter(20);
                    document.add(logoText);
                }
            } catch (Exception e) {
                log.warn("⚠️ Erreur chargement logo: {}", e.getMessage());
                // Fallback: logo texte à droite
                Font logoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new BaseColor(79, 70, 229));
                Paragraph logoText = new Paragraph("🎓 RH Formation", logoFont);
                logoText.setAlignment(Element.ALIGN_RIGHT);
                logoText.setSpacingAfter(20);
                document.add(logoText);
            }

            // Ligne de séparation
            Paragraph separator = new Paragraph("_______________________________________________");
            separator.setSpacingAfter(25);
            document.add(separator);

            // ==================== TITRE ====================
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);
            titleTable.setSpacingAfter(30);

            PdfPCell titleCell = new PdfPCell();
            titleCell.setBackgroundColor(new BaseColor(79, 70, 229));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPadding(20);

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32, BaseColor.WHITE);
            Paragraph title = new Paragraph("CERTIFICAT DE RÉUSSITE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(title);
            titleTable.addCell(titleCell);
            document.add(titleTable);

            // ==================== NOM DE L'EMPLOYÉ ====================
            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, new BaseColor(79, 70, 229));
            Paragraph name = new Paragraph(employe.getPrenom().toUpperCase() + " " + employe.getNom().toUpperCase(), nameFont);
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingBefore(20);
            name.setSpacingAfter(10);
            document.add(name);

            // ==================== TEXTE DE RÉUSSITE ====================
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 14, BaseColor.DARK_GRAY);
            Paragraph success = new Paragraph("a réussi l'examen", textFont);
            success.setAlignment(Element.ALIGN_CENTER);
            success.setSpacingAfter(10);
            document.add(success);

            // ==================== TITRE DE L'EXAMEN ====================
            Font examFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new BaseColor(79, 70, 229));
            Paragraph examTitle = new Paragraph(examen.getTitre(), examFont);
            examTitle.setAlignment(Element.ALIGN_CENTER);
            examTitle.setSpacingAfter(30);
            document.add(examTitle);

            // ==================== DÉTAILS ====================
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(70);
            detailsTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            detailsTable.setSpacingBefore(20);
            detailsTable.setSpacingAfter(30);

            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.DARK_GRAY);

            addDetailCell(detailsTable, "Note obtenue:", String.format("%.2f / 20", note), labelFont, valueFont);
            addDetailCell(detailsTable, "Pourcentage:", String.format("%.0f%%", note * 5), labelFont, valueFont);
            addDetailCell(detailsTable, "Date d'obtention:", dateCertification, labelFont, valueFont);
            addDetailCell(detailsTable, "ID Certification:", certificationId, labelFont, valueFont);

            document.add(detailsTable);

            // ==================== SIGNATURE ====================
            Font signatureFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph signature = new Paragraph("\n\nFait à Tunis, le " + dateCertification + "\n\n" +
                    "Le Responsable Formation\n" +
                    "_________________________", signatureFont);
            signature.setAlignment(Element.ALIGN_CENTER);
            document.add(signature);

            // ==================== QR CODE EN BAS À DROITE ====================
            if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) {
                byte[] qrBytes = Base64.getDecoder().decode(qrCodeBase64);
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.scaleToFit(100, 100);
                qrImage.setAbsolutePosition(450, 50);
                document.add(qrImage);

                // Ajouter un texte sous le QR Code
                Font qrTextFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
                Paragraph qrText = new Paragraph("Scannez pour vérifier l'authenticité", qrTextFont);
                qrText.setAlignment(Element.ALIGN_RIGHT);
                qrText.setSpacingBefore(5);
                document.add(qrText);
            }

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("❌ Erreur génération PDF: {}", e.getMessage());
            return null;
        }
    }

    private void addDetailCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(new BaseColor(245, 245, 245));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(8);
        table.addCell(valueCell);
    }

    private String sauvegarderPDF(byte[] pdfBytes, String certificationId, String employeId) {
        try {
            File dir = new File(storagePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = certificationId + "_" + employeId + ".pdf";
            String filePath = storagePath + File.separator + fileName;

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(pdfBytes);
            }

            return filePath;

        } catch (Exception e) {
            log.error("❌ Erreur sauvegarde PDF: {}", e.getMessage());
            return null;
        }
    }

    private void envoyerEmailAvecPDF(User employe, Examen examen, double note, String certificationId, String dateCertification, byte[] pdfBytes) {
        String sujet = "🎓 Votre certification - " + examen.getTitre();

        String message = String.format(
                """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #4F46E5;">Félicitations %s %s !</h2>
                        <p>Vous avez réussi l'examen <strong>"%s"</strong> avec une note de <strong>%.2f/20</strong>.</p>
                        <p>Votre certificat est joint à cet email.</p>
                        
                        <div style="background: #F3F4F6; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p><strong>📄 Informations :</strong></p>
                            <p>ID: %s</p>
                            <p>Date: %s</p>
                            <p>Note: %.2f/20 (%.0f%%)</p>
                        </div>
                        
                        <p>Conservez précieusement ce certificat.</p>
                        
                        <hr>
                        <p style="font-size: 12px; color: #6B7280;">
                            Cet email est une preuve officielle de votre certification.
                        </p>
                    </div>
                </body>
                </html>
                """,
                employe.getPrenom(), employe.getNom(),
                examen.getTitre(),
                note,
                certificationId,
                dateCertification,
                note, note * 5
        );

        notificationFormationService.envoyerEmailAvecPieceJointe(employe.getEmail(), sujet, message, pdfBytes, certificationId + ".pdf");
        log.info("📧 Email avec PDF envoyé à {}", employe.getEmail());
    }

    public Resource getCertificationPDF(String certificationId) {
        try {
            File pdfFile = new File(storagePath + File.separator + certificationId + ".pdf");
            if (pdfFile.exists()) {
                return new FileSystemResource(pdfFile);
            }
            return null;
        } catch (Exception e) {
            log.error("❌ Erreur récupération PDF: {}", e.getMessage());
            return null;
        }
    }

    public boolean hasCertification(String employeId, String formationId) {
        List<Examen> examens = examenRepository.findByFormationId(formationId);
        for (Examen examen : examens) {
            var resultat = resultatRepository.findByExamenIdAndEmployeId(examen.getId(), employeId);
            if (resultat.isPresent() && resultat.get().getNote() >= SEUIL_REUSSITE) {
                return true;
            }
        }
        return false;
    }
}