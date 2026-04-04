package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.ReservationNavette;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationNavetteRepository extends MongoRepository<ReservationNavette, String> {
    List<ReservationNavette> findByEmployeId(String employeId);
    List<ReservationNavette> findByBusId(String busId);
    List<ReservationNavette> findByStatut(StatutReservation statut);
    List<ReservationNavette> findByDateTrajet(LocalDate dateTrajet);  // ← Maintenant ça fonctionne
    long countByBusIdAndDateTrajet(String busId, LocalDate dateTrajet);
    long countByBusIdAndJoursSelectionnesContaining(String busId, String jour);
}