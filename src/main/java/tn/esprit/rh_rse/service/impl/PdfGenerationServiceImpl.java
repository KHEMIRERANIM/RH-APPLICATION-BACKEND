package tn.esprit.rh_rse.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.DetailsHotel;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.AvantageReservation;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.CategorieOffre;
import tn.esprit.rh_rse.service.PdfGenerationService;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PdfGenerationServiceImpl implements PdfGenerationService {

    // ── Palette couleurs ─────────────────────────────────────────────
    private static final Color C_VIOLET    = new Color(76,  29, 149);
    private static final Color C_INDIGO    = new Color(99, 102, 241);
    private static final Color C_INDIGO_LT = new Color(238,242, 255);
    private static final Color C_GREEN     = new Color(16, 185, 129);
    private static final Color C_GREEN_LT  = new Color(209,250, 229);
    private static final Color C_SLATE_800 = new Color(30,  41,  59);
    private static final Color C_SLATE_500 = new Color(100,116, 139);
    private static final Color C_SLATE_100 = new Color(241,245, 249);
    private static final Color C_BORDER    = new Color(226,232, 240);
    private static final Color C_WHITE     = Color.WHITE;

    @Override
    public byte[] generateReservationPdf(AvantageReservation reservation, Offre offre, User user) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4, 48, 48, 56, 48);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            boolean isHotel = CategorieOffre.HOTEL.equals(offre.getCategorie());
            Color accent   = isHotel ? C_GREEN   : C_INDIGO;
            Color accentLt = isHotel ? C_GREEN_LT : C_INDIGO_LT;

            // ── Polices ──────────────────────────────────────────────
            Font fBrand  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   20, C_VIOLET);
            Font fTitle  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   21, C_SLATE_800);
            Font fRef    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   12, accent);
            Font fSec    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11, accent);
            Font fKey    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11, C_SLATE_800);
            Font fVal    = FontFactory.getFont(FontFactory.HELVETICA,        11, C_SLATE_800);
            Font fMuted  = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, C_SLATE_500);
            Font fWhite  = FontFactory.getFont(FontFactory.HELVETICA,        11, C_WHITE);
            Font fWhiteB = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   14, C_WHITE);
            Font fBadge  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    8, C_WHITE);

            DateTimeFormatter dfFr = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
            DateTimeFormatter dfDt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm");

            // ════════════════════════════════════════════════════════
            // 1. EN-TÊTE
            // ════════════════════════════════════════════════════════
            PdfPTable tHead = new PdfPTable(2);
            tHead.setWidthPercentage(100);
            tHead.setWidths(new float[]{1.6f, 1f});
            tHead.setSpacingAfter(6f);

            PdfPCell cBrand = new PdfPCell();
            cBrand.setBorder(Rectangle.NO_BORDER);
            cBrand.addElement(new Paragraph("ENTREPRISE", fBrand));
            cBrand.addElement(new Paragraph("Service Mutuelle & Avantages Sociaux", fMuted));
            tHead.addCell(cBrand);

            String badgeTxt = isHotel ? "RESERVATION HOTELIERE" : "RESERVATION " + offre.getCategorie().name();
            PdfPCell cBadge = new PdfPCell(new Phrase(badgeTxt, fBadge));
            cBadge.setBackgroundColor(accent);
            cBadge.setBorder(Rectangle.NO_BORDER);
            cBadge.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cBadge.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cBadge.setPadding(7f);
            tHead.addCell(cBadge);
            doc.add(tHead);

            LineSeparator lsThick = new LineSeparator(2.5f, 100f, accent, Element.ALIGN_CENTER, 0);
            LineSeparator lsThin  = new LineSeparator(0.8f, 100f, C_BORDER, Element.ALIGN_CENTER, 0);
            doc.add(new Chunk(lsThick));
            doc.add(Chunk.NEWLINE);

            // ════════════════════════════════════════════════════════
            // 2. TITRE + REFERENCE
            // ════════════════════════════════════════════════════════
            String refId = reservation.getId() != null
                    ? "#" + reservation.getId().substring(Math.max(0, reservation.getId().length() - 8)).toUpperCase()
                    : "#N/A";
            String dateEmis = reservation.getDateReservation() != null
                    ? reservation.getDateReservation().format(dfDt) : "-";

            Paragraph pTitle = new Paragraph("CONFIRMATION DE RESERVATION", fTitle);
            pTitle.setSpacingBefore(6f);
            pTitle.setSpacingAfter(3f);
            doc.add(pTitle);

            Paragraph pRef = new Paragraph("Reference  " + refId + "   |   Emise le " + dateEmis, fRef);
            pRef.setSpacingAfter(18f);
            doc.add(pRef);

            // ════════════════════════════════════════════════════════
            // 3. INFORMATIONS DE L'EMPLOYE
            // ════════════════════════════════════════════════════════
            doc.add(sectionTitle("INFORMATIONS DE L'EMPLOYE", fSec, accentLt));
            doc.add(Chunk.NEWLINE);

            PdfPTable tEmp = twoCol();
            row(tEmp, "Nom complet",   user.getPrenom() + " " + user.getNom(), fKey, fVal, accentLt);
            row(tEmp, "Email",         user.getEmail(),                         fKey, fVal, accentLt);
            tEmp.setSpacingAfter(14f);
            doc.add(tEmp);

            doc.add(new Chunk(lsThin));
            doc.add(Chunk.NEWLINE);

            // ════════════════════════════════════════════════════════
            // 4. DETAIL DE L'OFFRE
            // ════════════════════════════════════════════════════════
            doc.add(sectionTitle("DETAIL DE L'OFFRE", fSec, accentLt));
            doc.add(Chunk.NEWLINE);

            PdfPTable tOffre = twoCol();
            row(tOffre, "Nom de l'offre", offre.getTitre(), fKey, fVal, accentLt);
            row(tOffre, "Categorie",       offre.getCategorie().name(), fKey, fVal, accentLt);
            if (offre.getLocalisation() != null && !offre.getLocalisation().isBlank()) {
                row(tOffre, "Lieu / Destination", offre.getLocalisation(), fKey, fVal, accentLt);
            }
            if (offre.getDescription() != null && !offre.getDescription().isBlank()) {
                String d = offre.getDescription();
                if (d.length() > 80) d = d.substring(0, 77) + "...";
                row(tOffre, "Description", d, fKey, fVal, accentLt);
            }
            tOffre.setSpacingAfter(14f);
            doc.add(tOffre);

            doc.add(new Chunk(lsThin));
            doc.add(Chunk.NEWLINE);

            // ════════════════════════════════════════════════════════
            // 5. DETAIL DU SEJOUR (HOTEL UNIQUEMENT)
            // ════════════════════════════════════════════════════════
            if (isHotel) {
                doc.add(sectionTitle("DETAIL DU SEJOUR HOTELIER", fSec, accentLt));
                doc.add(Chunk.NEWLINE);

                PdfPTable tH = twoCol();

                String ciStr = reservation.getCheckIn()  != null ? reservation.getCheckIn().format(dfFr)  : "-";
                String coStr = reservation.getCheckOut() != null ? reservation.getCheckOut().format(dfFr) : "-";

                long nuits = 0;
                if (reservation.getCheckIn() != null && reservation.getCheckOut() != null) {
                    nuits = ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
                }

                int nbA = reservation.getNbAdultes() != null ? reservation.getNbAdultes() : 0;
                int nbE = reservation.getNbEnfants() != null ? reservation.getNbEnfants() : 0;

                row(tH, "Arrivee (Check-in)",      ciStr,                  fKey, fVal, accentLt);
                row(tH, "Depart (Check-out)",       coStr,                  fKey, fVal, accentLt);
                row(tH, "Duree du sejour",           nuits + " nuit(s)",    fKey, fVal, accentLt);
                row(tH, "Adultes",                   nbA   + " adulte(s)",  fKey, fVal, accentLt);
                row(tH, "Enfants",                   nbE   + " enfant(s)",  fKey, fVal, accentLt);
                row(tH, "Total personnes",            (nbA + nbE) + " personne(s)", fKey, fVal, accentLt);

                String form = reservation.getFormule();
                String formFull = "-";
                if ("PD".equals(form)) formFull = "Petit Dejeuner inclus (PD)";
                else if ("DP".equals(form)) formFull = "Demi-Pension : Petit dejeuner + Diner (DP)";
                else if ("PC".equals(form)) formFull = "Pension Complete : 3 repas inclus (PC)";
                row(tH, "Formule de pension", formFull, fKey, fVal, accentLt);

                DetailsHotel dh = offre.getDetailsHotel();
                if (dh != null) {
                    row(tH, "Tarif adulte / nuit",
                            String.format("%.2f DT", dh.getPrixAdulte() != null ? dh.getPrixAdulte() : 0.0),
                            fKey, fVal, accentLt);
                    row(tH, "Tarif enfant / nuit",
                            String.format("%.2f DT", dh.getPrixEnfant() != null ? dh.getPrixEnfant() : 0.0),
                            fKey, fVal, accentLt);
                    if (form != null && dh.getSurprixFormules() != null && dh.getSurprixFormules().containsKey(form)) {
                        double sp = dh.getSurprixFormules().get(form);
                        if (sp > 0) {
                            row(tH, "Supplement pension", String.format("+%.2f DT/pers/nuit", sp), fKey, fVal, accentLt);
                        }
                    }
                }

                tH.setSpacingAfter(14f);
                doc.add(tH);

                doc.add(new Chunk(lsThin));
                doc.add(Chunk.NEWLINE);
            }

            // ════════════════════════════════════════════════════════
            // 6. RECAPITULATIF FINANCIER
            // ════════════════════════════════════════════════════════
            doc.add(sectionTitle("RECAPITULATIF FINANCIER", fSec, accentLt));
            doc.add(Chunk.NEWLINE);

            PdfPTable tFin = new PdfPTable(3);
            tFin.setWidthPercentage(100);
            tFin.setWidths(new float[]{2.5f, 1.4f, 1.1f});
            tFin.setSpacingAfter(10f);

            hCell(tFin, "Designation", fKey);
            hCell(tFin, "Quantite",    fKey);
            hCell(tFin, "Montant",     fKey);

            if (isHotel) {
                long nuits = 0;
                if (reservation.getCheckIn() != null && reservation.getCheckOut() != null) {
                    nuits = ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
                }
                int nbA = reservation.getNbAdultes() != null ? reservation.getNbAdultes() : 0;
                int nbE = reservation.getNbEnfants() != null ? reservation.getNbEnfants() : 0;
                DetailsHotel dh = offre.getDetailsHotel();
                double pA = dh != null && dh.getPrixAdulte() != null ? dh.getPrixAdulte() : 0;
                double pE = dh != null && dh.getPrixEnfant() != null ? dh.getPrixEnfant() : 0;

                if (nbA > 0) {
                    dCell(tFin, offre.getTitre() + " - Adultes", fVal);
                    dCell(tFin, nbA + " x " + nuits + " nuit(s)", fVal);
                    dCell(tFin, String.format("%.2f DT", nbA * pA * nuits), fVal);
                }
                if (nbE > 0) {
                    dCell(tFin, offre.getTitre() + " - Enfants", fVal);
                    dCell(tFin, nbE + " x " + nuits + " nuit(s)", fVal);
                    dCell(tFin, String.format("%.2f DT", nbE * pE * nuits), fVal);
                }
            } else {
                double pC = offre.getPrixConvention() != null ? offre.getPrixConvention() : 0;
                dCell(tFin, offre.getTitre(), fVal);
                dCell(tFin, reservation.getNbPersonnes() + " personne(s)", fVal);
                dCell(tFin, String.format("%.2f DT", pC * reservation.getNbPersonnes()), fVal);
            }

            doc.add(tFin);

            // ── Bandeau TOTAL ────────────────────────────────────────
            PdfPTable tTotal = new PdfPTable(2);
            tTotal.setWidthPercentage(55);
            tTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tTotal.setWidths(new float[]{1.6f, 1f});
            tTotal.setSpacingAfter(22f);

            PdfPCell cLbl = new PdfPCell(new Phrase("TOTAL A REGLER (TTC)", fWhite));
            cLbl.setBackgroundColor(accent);
            cLbl.setPadding(10f);
            cLbl.setBorder(Rectangle.NO_BORDER);
            tTotal.addCell(cLbl);

            double total = reservation.getPrixTotal() != null ? reservation.getPrixTotal() : 0;
            PdfPCell cAmt = new PdfPCell(new Phrase(String.format("%.2f DT", total), fWhiteB));
            cAmt.setBackgroundColor(accent);
            cAmt.setPadding(10f);
            cAmt.setBorder(Rectangle.NO_BORDER);
            cAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tTotal.addCell(cAmt);
            doc.add(tTotal);

            // ════════════════════════════════════════════════════════
            // 7. PIED DE PAGE
            // ════════════════════════════════════════════════════════
            doc.add(new Chunk(lsThin));
            doc.add(Chunk.NEWLINE);

            Paragraph foot1 = new Paragraph(
                "Ce document constitue une preuve officielle de reservation. " +
                "Veuillez le presenter a l'accueil de l'etablissement ou a notre administration.", fMuted);
            foot1.setAlignment(Element.ALIGN_CENTER);
            foot1.setSpacingAfter(4f);
            doc.add(foot1);

            Paragraph foot2 = new Paragraph(
                "Ref. " + refId + "  -  Document genere automatiquement par le Systeme RH-RSE.",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, C_SLATE_500));
            foot2.setAlignment(Element.ALIGN_CENTER);
            doc.add(foot2);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la creation du PDF de reservation", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Paragraph sectionTitle(String text, Font font, Color bg) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(2f);
        p.setSpacingAfter(4f);
        return p;
    }

    private PdfPTable twoCol() throws RuntimeException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        try { t.setWidths(new float[]{1.4f, 2f}); } catch (Exception ignored) {}
        return t;
    }

    private void row(PdfPTable t, String key, String val, Font fK, Font fV, Color bg) {
        PdfPCell ck = new PdfPCell(new Phrase(key, fK));
        ck.setBorder(Rectangle.NO_BORDER);
        ck.setBackgroundColor(C_SLATE_100);
        ck.setPadding(7f);
        ck.setPaddingLeft(10f);
        t.addCell(ck);

        PdfPCell cv = new PdfPCell(new Phrase(val, fV));
        cv.setBorder(Rectangle.NO_BORDER);
        cv.setPadding(7f);
        cv.setPaddingLeft(10f);
        t.addCell(cv);
    }

    private void hCell(PdfPTable t, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(C_SLATE_100);
        c.setPadding(8f);
        c.setBorderColor(C_BORDER);
        t.addCell(c);
    }

    private void dCell(PdfPTable t, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(8f);
        c.setBorderColor(C_BORDER);
        t.addCell(c);
    }
}
