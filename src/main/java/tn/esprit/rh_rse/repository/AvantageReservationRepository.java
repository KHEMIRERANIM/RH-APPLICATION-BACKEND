package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.AvantageReservation;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.util.List;
import java.util.Optional;

public interface AvantageReservationRepository extends MongoRepository<AvantageReservation, String> {

    List<AvantageReservation> findByIdUser(String idUser);

    List<AvantageReservation> findByIdOffreAvantage(String idOffreAvantage);

    Optional<AvantageReservation> findByIdUserAndIdOffreAvantageAndStatut(
            String idUser, String idOffreAvantage, StatutReservation statut);

    void deleteByIdUserAndStatut(String idUser, StatutReservation statut);
}
