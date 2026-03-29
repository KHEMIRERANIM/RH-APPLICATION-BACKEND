package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.AvantageReservation;
import tn.esprit.rh_rse.entity.User;

public interface PdfGenerationService {
    byte[] generateReservationPdf(AvantageReservation reservation, Offre offre, User user);
}
