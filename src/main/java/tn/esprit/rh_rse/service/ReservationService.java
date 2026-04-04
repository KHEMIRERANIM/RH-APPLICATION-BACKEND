package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.dto.request.DemandeRemplacementCovoiturageRequest;
import tn.esprit.rh_rse.dto.request.ReservationRequest;
import tn.esprit.rh_rse.dto.response.ReservationResponse;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.util.List;

public interface ReservationService {
    List<ReservationResponse> getAll();
    ReservationResponse getById(String id);
    ReservationResponse create(ReservationRequest request);
    ReservationResponse update(String id, ReservationRequest request);
    void delete(String id);
    List<ReservationResponse> getByEmployeId(String employeId);
    List<ReservationResponse> getByTrajetId(String trajetId);
    List<ReservationResponse> getByStatut(StatutReservation statut);
    int getTotalPointsEcoByEmploye(String employeId);

    /** Demande de remplacement (alternatives) : nouvelle réservation EN_ATTENTE + notification conducteur ; l'ancienne est annulée à la confirmation */
    ReservationResponse demanderRemplacementCovoiturage(DemandeRemplacementCovoiturageRequest request);
}