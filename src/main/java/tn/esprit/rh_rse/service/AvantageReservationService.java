package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.AvantageReservation;

import java.util.List;
import java.time.LocalDate;

public interface AvantageReservationService {

    /** Réservation standard (VOYAGE / FESTIVAL) */
    AvantageReservation reserverOuModifier(String idUser, String idOffre, Integer nbPersonnes);

    /** Réservation hôtelière avec adultes, enfants et formule pension */
    AvantageReservation reserverHotel(String idUser, String idOffre,
                              Integer nbAdultes, Integer nbEnfants, String formule, LocalDate checkIn, LocalDate checkOut);

    AvantageReservation annuler(String idUser, String idReservation);
    List<AvantageReservation> getMesReservations(String idUser);
    List<AvantageReservation> getAllReservations();
    List<AvantageReservation> getByOffre(String idOffre);
    void viderAnnulees(String idUser);
    AvantageReservation creerReservationHotel(String idUser, String idOffre, Integer nbPersonnesChoisi);
}
