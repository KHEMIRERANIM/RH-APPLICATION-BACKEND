package tn.esprit.rh_rse.service.conge;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.response.conge.BulletinSalaireResponse;
import tn.esprit.rh_rse.entity.User;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGeneratorService {

    public byte[] genererBulletinPDF(BulletinSalaireResponse bulletin, User employe) {
        try {
            System.out.println("📄 PDF Generator - Début");
            System.out.println("  - Bulletin ID: " + bulletin.getId());
            System.out.println("  - Employé: " + employe.getEmail());
            System.out.println("  - Mois: " + bulletin.getMois());
            System.out.println("  - Année: " + bulletin.getAnnee());
            System.out.println("  - Salaire Brut: " + bulletin.getSalaireBrut());
            System.out.println("  - Salaire Net: " + bulletin.getSalaireNet());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Titre
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("BULLETIN DE SALAIRE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Informations employé (avec gestion des null)
            document.add(new Paragraph("Employé : " + safeString(employe.getPrenom()) + " " + safeString(employe.getNom())));
            document.add(new Paragraph("Email : " + safeString(employe.getEmail())));
            document.add(new Paragraph("Poste : " + safeString(employe.getPoste())));
            document.add(new Paragraph("Département : " + safeString(employe.getDepartement())));
            document.add(new Paragraph("Période : " + getMoisLabel(bulletin.getMois()) + " " + bulletin.getAnnee()));
            document.add(Chunk.NEWLINE);

            // Tableau
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableRow(table, "Salaire Brut", String.format("%.2f TND", bulletin.getSalaireBrut()));
            addTableRow(table, "Primes", String.format("%.2f TND", bulletin.getPrimes()));
            addTableRow(table, "Heures Supplémentaires", String.format("%.2f TND", bulletin.getHeuresSupplementaires()));
            addTableRow(table, "CNSS (9.18%)", String.format("-%.2f TND", bulletin.getCotisationsCNSS()));
            addTableRow(table, "IRPP (15%)", String.format("-%.2f TND", bulletin.getIrpp()));
            addTableRow(table, "Autres Retenues", String.format("-%.2f TND", bulletin.getAutresRetenues()));

            // Total
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            PdfPCell cell1 = new PdfPCell(new Phrase("SALAIRE NET", boldFont));
            PdfPCell cell2 = new PdfPCell(new Phrase(String.format("%.2f TND", bulletin.getSalaireNet()), boldFont));
            cell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell1);
            table.addCell(cell2);

            document.add(table);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Généré le : " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

            document.close();
            System.out.println("✅ PDF Generator - Succès, taille: " + out.size() + " bytes");
            return out.toByteArray();

        } catch (Exception e) {
            System.err.println("❌ PDF Generator - Erreur: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur génération PDF: " + e.getMessage());
        }
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        table.addCell(new PdfPCell(new Phrase(label)));
        table.addCell(new PdfPCell(new Phrase(value)));
    }

    private String getMoisLabel(int mois) {
        String[] moisLabels = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        return moisLabels[mois - 1];
    }

    private String safeString(String value) {
        return value != null ? value : "-";
    }
}