// repository/Formation/FraudEventRepository.java
package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.FraudEvent;

import java.util.List;

@Repository
public interface FraudEventRepository extends MongoRepository<FraudEvent, String> {

    // Compter les violations
    @Query(value = "{ 'examenId': ?0, 'employeId': ?1 }", count = true)
    long countViolations(String examenId, String employeId);

    // Alternative sans @Query
    long countByExamenIdAndEmployeId(String examenId, String employeId);

    // Trouver les événements d'un examen
    List<FraudEvent> findByExamenIdOrderByTimestampDesc(String examenId);

    // Trouver les événements d'un étudiant
    List<FraudEvent> findByExamenIdAndEmployeIdOrderByTimestampDesc(String examenId, String employeId);

    // Trouver les événements bloqués
    @Query(value = "{ 'examenId': ?0, 'employeId': ?1, 'isBlocked': true }")
    List<FraudEvent> findBlockedEvents(String examenId, String employeId);

    // Par type d'événement
    List<FraudEvent> findByEventType(String eventType);

    // Supprimer par examen
    void deleteByExamenId(String examenId);
}