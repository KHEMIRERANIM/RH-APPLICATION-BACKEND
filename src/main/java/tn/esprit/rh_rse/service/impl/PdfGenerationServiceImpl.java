package tn.esprit.rh_rse.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.AvantageReservation;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.service.PdfGenerationService;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfGenerationServiceImpl implements PdfGenerationService {

    @Override
    public byte[] generateReservationPdf(AvantageReservation reservation, Offre offre, User user) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Couleurs
            Color primaryColor = new Color(76, 29, 149); // Violet foncé
            Color accentColor = new Color(99, 102, 241); // Indigo
            Color textMain = new Color(30, 41, 59); // Slate 800
            Color textMuted = new Color(100, 116, 139); // Slate 500

            // Polices
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, primaryColor);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, accentColor);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, textMain);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, textMain);
            Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, textMuted);

            // 1. En-tête : Logo / Titre Entreprise
            Paragraph company = new Paragraph("ENTREPRISE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, primaryColor));
            company.setAlignment(Element.ALIGN_RIGHT);
            document.add(company);
            
            Paragraph badge = new Paragraph("Service Mutuelle & Avantages Sociaux", mutedFont);
            badge.setAlignment(Element.ALIGN_RIGHT);
            document.add(badge);

            document.add(Chunk.NEWLINE);

            // 2. Titre du document
            Paragraph title = new Paragraph("FICHE DE RÉSERVATION", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            document.add(title);
            
            String idResText = reservation.getId() != null ? reservation.getId().substring(reservation.getId().length() - 8).toUpperCase() : "N/A";
            Paragraph ref = new Paragraph("Référence : #" + idResText, subTitleFont);
            ref.setAlignment(Element.ALIGN_LEFT);
            ref.setSpacingAfter(20);
            document.add(ref);

            // Ligne séparatrice
            LineSeparator ls = new LineSeparator();
            ls.setLineColor(new Color(226, 232, 240));
            document.add(new Chunk(ls));
            document.add(Chunk.NEWLINE);

            // 3. Bloc Informations de l'Employé
            document.add(new Paragraph("INFORMATIONS DE L'EMPLOYÉ", subTitleFont));
            document.add(Chunk.NEWLINE);

            PdfPTable tableEmp = new PdfPTable(2);
            tableEmp.setWidthPercentage(100);
            tableEmp.setSpacingAfter(20f);
            
            addCellDefinition(tableEmp, "Nom Complet :", headerFont);
            addCellDefinition(tableEmp, user.getPrenom() + " " + user.getNom(), normalFont);
            
            addCellDefinition(tableEmp, "Email :", headerFont);
            addCellDefinition(tableEmp, user.getEmail(), normalFont);
            
            document.add(tableEmp);

            document.add(new Chunk(ls));
            document.add(Chunk.NEWLINE);

            // 4. Bloc Détails de l'Offre
            document.add(new Paragraph("DÉTAIL DE L'OFFRE RÉSERVÉE", subTitleFont));
            document.add(Chunk.NEWLINE);

            PdfPTable tableOffre = new PdfPTable(2);
            tableOffre.setWidthPercentage(100);
            tableOffre.setSpacingAfter(20f);

            addCellDefinition(tableOffre, "Offre :", headerFont);
            addCellDefinition(tableOffre, offre.getTitre(), normalFont);

            addCellDefinition(tableOffre, "Description :", headerFont);
            String desc = offre.getDescription() != null ? offre.getDescription() : "-";
            if (desc.length() > 60) desc = desc.substring(0, 57) + "...";
            addCellDefinition(tableOffre, desc, normalFont);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String dateRes = reservation.getDateReservation() != null ? reservation.getDateReservation().format(dtf) : "-";
            
            addCellDefinition(tableOffre, "Date de réservation :", headerFont);
            addCellDefinition(tableOffre, dateRes, normalFont);

            document.add(tableOffre);

            document.add(new Chunk(ls));
            document.add(Chunk.NEWLINE);

            // 5. Bloc Facturation / Résumé des coûts
            document.add(new Paragraph("RÉSUMÉ FINAL", subTitleFont));
            document.add(Chunk.NEWLINE);

            PdfPTable tablePrix = new PdfPTable(3);
            tablePrix.setWidthPercentage(100);
            tablePrix.setWidths(new float[]{2f, 1f, 1f});

            // En-têtes du tableau
            PdfPCell cellDesc = new PdfPCell(new Phrase("Description", headerFont));
            cellDesc.setBackgroundColor(new Color(241, 245, 249));
            cellDesc.setPadding(8f);
            tablePrix.addCell(cellDesc);

            PdfPCell cellQte = new PdfPCell(new Phrase("Nb Personnes", headerFont));
            cellQte.setBackgroundColor(new Color(241, 245, 249));
            cellQte.setPadding(8f);
            tablePrix.addCell(cellQte);

            PdfPCell cellTotal = new PdfPCell(new Phrase("Prix Total", headerFont));
            cellTotal.setBackgroundColor(new Color(241, 245, 249));
            cellTotal.setPadding(8f);
            tablePrix.addCell(cellTotal);

            // Données
            PdfPCell c1 = new PdfPCell(new Phrase(offre.getTitre() + " (" + offre.getPrixConvention() + " DT / pers.)", normalFont));
            c1.setPadding(8f);
            tablePrix.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(reservation.getNbPersonnes()), normalFont));
            c2.setPadding(8f);
            tablePrix.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase(reservation.getPrixTotal() + " DT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primaryColor)));
            c3.setPadding(8f);
            tablePrix.addCell(c3);

            document.add(tablePrix);

            // Pied de page
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Merci de présenter ce document comme preuve de confirmation auprès de notre administration ou du partenaire concerné.", mutedFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du PDF", e);
        }
    }

    private void addCellDefinition(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setPaddingBottom(10f);
        table.addCell(cell);
    }
}
