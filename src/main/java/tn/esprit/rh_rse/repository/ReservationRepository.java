package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Reservation;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends MongoRepository<Reservation, String> {

    List<Reservation> findByIdUser(String idUser);

    List<Reservation> findByIdOffre(String idOffre);

    Optional<Reservation> findByIdUserAndIdOffreAndStatut(
            String idUser, String idOffre, StatutReservation statut);
}