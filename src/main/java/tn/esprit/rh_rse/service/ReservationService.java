package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Reservation;

import java.util.List;
import java.time.LocalDate;

public interface ReservationService {

    /** Réservation standard (VOYAGE / FESTIVAL) */
    Reservation reserverOuModifier(String idUser, String idOffre, Integer nbPersonnes);

    /** Réservation hôtelière avec adultes, enfants et formule pension */
    Reservation reserverHotel(String idUser, String idOffre,
                              Integer nbAdultes, Integer nbEnfants, String formule, LocalDate checkIn, LocalDate checkOut);

    Reservation annuler(String idUser, String idReservation);
    List<Reservation> getMesReservations(String idUser);
    List<Reservation> getAllReservations();
    List<Reservation> getByOffre(String idOffre);
    void viderAnnulees(String idUser);
    Reservation creerReservationHotel(String idUser, String idOffre, Integer nbPersonnesChoisi);
}