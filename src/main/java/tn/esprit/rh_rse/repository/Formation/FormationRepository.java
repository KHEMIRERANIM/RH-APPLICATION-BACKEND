package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.Formation;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FormationRepository extends MongoRepository<Formation, String> {

    List<Formation> findByActiveTrue();

    List<Formation> findByType(String type);

    @Query("{ 'dateDebut': { $gt: ?0 }, 'placesDisponibles': { $gt: 0 }, 'active': true }")
    List<Formation> findFormationsDisponibles(LocalDateTime now);

    @Query("{ 'dateDebut': { $gte: ?0, $lte: ?1 } }")
    List<Formation> findFormationsByPeriode(LocalDateTime start, LocalDateTime end);

    List<Formation> findByFormateur(String formateur);
}