package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.AvantageReservation;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.util.List;
import java.util.Optional;

public interface AvantageReservationRepository extends MongoRepository<AvantageReservation, String> {

    List<AvantageReservation> findByIdUser(String idUser);

    List<AvantageReservation> findByIdOffre(String idOffre);

    Optional<AvantageReservation> findByIdUserAndIdOffreAndStatut(
            String idUser, String idOffre, StatutReservation statut);

    void deleteByIdUserAndStatut(String idUser, StatutReservation statut);
}
