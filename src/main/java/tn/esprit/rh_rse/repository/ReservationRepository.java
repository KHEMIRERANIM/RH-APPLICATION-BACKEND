package tn.esprit.rh_rse.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Reservation;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends MongoRepository<Reservation, String> {

    // Par employé
    List<Reservation> findByEmployeId(String employeId);

    // Par trajet
    List<Reservation> findByTrajetId(String trajetId);

    // Par statut
    List<Reservation> findByStatut(StatutReservation statut);

    // Par date de calcul CO2
    List<Reservation> findByDateCalcul(LocalDate dateCalcul);

    // Par employé ET statut
    List<Reservation> findByEmployeIdAndStatut(String employeId, StatutReservation statut);

    // Par trajet ET statut
    List<Reservation> findByTrajetIdAndStatut(String trajetId, StatutReservation statut);

    // Compter les réservations d'un trajet
    long countByTrajetId(String trajetId);

    // Compter par statut
    long countByStatut(StatutReservation statut);


}