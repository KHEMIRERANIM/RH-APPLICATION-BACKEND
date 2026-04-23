package tn.esprit.rh_rse.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Commande;
import tn.esprit.rh_rse.repository.CommandeRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
public class ExportCommandeController {

    private final CommandeRepository commandeRepository;
    private final UserRepository userRepository;

    @GetMapping("/export/pdf")
    public void exportPdf(HttpServletResponse response) throws IOException, DocumentException {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=commandes_" + LocalDate.now() + ".pdf");

        List<Commande> commandes = commandeRepository.findAll();

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font titreFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(67, 97, 238));
        Paragraph titre = new Paragraph("Rapport des Commandes", titreFont);
        titre.setAlignment(Element.ALIGN_CENTER);
        document.add(titre);

        Font dateFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.GRAY);
        Paragraph date = new Paragraph("Généré le : " + LocalDate.now(), dateFont);
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 2f, 2f, 2f});

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
        BaseColor headerColor = new BaseColor(67, 97, 238);
        String[] headers = {"Employé", "Date", "Montant (TND)", "Statut"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        Font rowFont = new Font(Font.FontFamily.HELVETICA, 10);
        double total = 0;
        boolean pair = false;
        BaseColor gris = new BaseColor(245, 245, 250);

        for (Commande c : commandes) {
            String nomUser = userRepository.findById(c.getUserId())
                    .map(u -> u.getPrenom() + " " + u.getNom())
                    .orElse(c.getUserId());

            String dateCommande = c.getDateCommande() != null
                    ? c.getDateCommande().toString()
                    : "-";

            double montant = c.getMontantTotal() != null ? c.getMontantTotal() : 0.0;

            String[] values = {
                    nomUser,
                    dateCommande,
                    String.format("%.2f", montant),
                    c.getStatut() != null ? c.getStatut() : "-"
            };

            for (String v : values) {
                PdfPCell cell = new PdfPCell(new Phrase(v, rowFont));
                cell.setPadding(7);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                if (pair) cell.setBackgroundColor(gris);
                table.addCell(cell);
            }
            total += montant;
            pair = !pair;
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        Font totalFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, new BaseColor(67, 97, 238));
        Paragraph totalPara = new Paragraph(
                String.format("Total encaissé : %.2f TND", total), totalFont
        );
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        document.add(totalPara);

        document.add(Chunk.NEWLINE);
        Font statsFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.GRAY);
        document.add(new Paragraph("Nombre total de commandes : " + commandes.size(), statsFont));
        document.add(new Paragraph(
                "Commandes livrées : " + commandes.stream()
                        .filter(c -> "livree".equals(c.getStatut())).count(),
                statsFont
        ));

        document.close();
    }
}