package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Reservation;

import java.util.List;

public interface ReservationService {
    Reservation reserverOuModifier(String idUser, String idOffre, Integer nbPersonnes);
    Reservation annuler(String idUser, String idReservation);
    List<Reservation> getMesReservations(String idUser);
    List<Reservation> getAllReservations();
    List<Reservation> getByOffre(String idOffre);
}