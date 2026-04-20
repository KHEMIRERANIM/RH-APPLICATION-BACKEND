package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.OffreAvantage;
import tn.esprit.rh_rse.entity.AvantageReservation;
import tn.esprit.rh_rse.entity.User;

public interface PdfGenerationService {
    byte[] generateReservationPdf(AvantageReservation reservation, OffreAvantage offreAvantage, User user);
}
