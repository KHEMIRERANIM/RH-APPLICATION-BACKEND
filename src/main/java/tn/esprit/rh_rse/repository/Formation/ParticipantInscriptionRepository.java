// ParticipantInscriptionRepository.java
package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.ParticipantInscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantInscriptionRepository extends MongoRepository<ParticipantInscription, String> {

    List<ParticipantInscription> findByFormationId(String formationId);
    List<ParticipantInscription> findByEmployeId(String employeId);
    @Query(value = "{ '_id' : { $in: ?0 } }", delete = true)
    void deleteAllByIds(List<String> ids);
    Optional<ParticipantInscription> findByFormationIdAndEmployeId(String formationId, String employeId);

    List<ParticipantInscription> findByFormationIdAndStatut(String formationId, String statut);

    List<ParticipantInscription> findByFormationIdAndPresenceValidee(String formationId, Boolean presenceValidee);
    // ParticipantInscriptionRepository.java
    boolean existsByFormationIdAndEmployeId(String formationId, String employeId);

    long countByFormationIdAndStatut(String formationId, String statut);

    long countByFormationIdAndPresenceValidee(String formationId, Boolean presenceValidee);

    @Query("{ 'employeId': ?0, 'statut': 'ANNULE', 'dateAnnulation': { $gte: ?1, $lte: ?2 } }")
    long countAnnulationsByEmployeInPeriod(String employeId, LocalDateTime debut, LocalDateTime fin);
}