package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.InscriptionFormation;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscriptionFormationRepository extends MongoRepository<InscriptionFormation, String> {

    // Méthodes existantes
    List<InscriptionFormation> findByEmployeId(String employeId);

    List<InscriptionFormation> findByFormationId(String formationId);

    @Query("{ 'formationId': ?0, 'employeId': ?1 }")
    List<InscriptionFormation> findByFormationIdAndEmployeId(String formationId, String employeId);

    @Query(value = "{ 'formationId': ?0, 'statut': 'CONFIRME' }", count = true)
    int countInscriptionsConfirmees(String formationId);

    boolean existsByFormationIdAndEmployeId(String formationId, String employeId);

    List<InscriptionFormation> findByFormationIdAndStatut(String formationId, String statut);

    void deleteByFormationId(String formationId);

    // ✅ NOUVEAU: Trouver par statut
    List<InscriptionFormation> findByStatut(String statut);

    // ✅ NOUVEAU: Trouver par employeId et statut
    List<InscriptionFormation> findByEmployeIdAndStatut(String employeId, String statut);

    // ✅ NOUVEAU: Trouver par formationId et employeId avec statut spécifique
    @Query("{ 'formationId': ?0, 'employeId': ?1, 'statut': { $in: ?2 } }")
    List<InscriptionFormation> findByFormationIdAndEmployeIdAndStatutIn(
            String formationId, String employeId, List<String> statuts);

    // ✅ NOUVEAU: Compter par formationId et statut
    @Query(value = "{ 'formationId': ?0, 'statut': ?1 }", count = true)
    long countByFormationIdAndStatut(String formationId, String statut);
    boolean existsByFormationIdAndEmployeIdAndPresenceConfirmeeTrue(String formationId, String employeId);
    // ✅ NOUVEAU: Trouver les inscriptions d'un employé avec statut CONFIRME ou TERMINE
    @Query("{ 'employeId': ?0, 'statut': { $in: ['CONFIRME', 'TERMINE'] } }")
    List<InscriptionFormation> findCompletedByEmployeId(String employeId);
    // InscriptionFormationRepository.java
    default Optional<InscriptionFormation> findByFormationIdAndEmployeIdSingle(String formationId, String employeId) {
        List<InscriptionFormation> list = findByFormationIdAndEmployeId(formationId, employeId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }



    // CORRIGÉ - Méthode pour récupérer les inscriptions actives (non terminées et non annulées)
    @Query("SELECT i FROM InscriptionFormation i WHERE i.statut != 'TERMINE' AND i.statut != 'ANNULE'")
    List<InscriptionFormation> findActiveInscriptions();

    // Alternative avec NOT IN (corrigé)
    @Query("SELECT i FROM InscriptionFormation i WHERE i.statut NOT IN (statuts)")
    List<InscriptionFormation> findByStatutNotIn(@Param("statuts") List<String> statuts);

    // Méthode dérivée simple
    List<InscriptionFormation> findByStatutNot(String statut);

    // Trouver par formation et statut
}