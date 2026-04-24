package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.Avis;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvisRepository extends MongoRepository<Avis, String> {

    // ========== MÉTHODES EXISTANTES ==========
    List<Avis> findByFormationId(String formationId);

    List<Avis> findByEmployeId(String employeId);

    List<Avis> findByFormationIdAndValide(String formationId, Boolean valide);

    boolean existsByFormationIdAndEmployeId(String formationId, String employeId);

    void deleteByFormationId(String formationId);

    long countByFormationIdAndNote(String formationId, Integer note);

    List<Avis> findByFormationIdOrderByCreatedAtDesc(String formationId);

    Optional<Avis> findByFormationIdAndEmployeId(String formationId, String employeId);

    List<Avis> findByValideFalseOrderByCreatedAtDesc();

    @Query("{ 'formationId': ?0, 'valide': true }")
    List<Avis> findValidatedByFormationId(String formationId);

    // ========== NOUVELLES MÉTHODES POUR LE SENTIMENT ==========

    /**
     * Récupère tous les avis validés (pour l'analyse globale)
     */
    @Query("{ 'valide': true }")
    List<Avis> findAllValidAvis();

    /**
     * Récupère tous les avis triés par date décroissante
     */
    @Query(value = "{}", sort = "{ 'createdAt': -1 }")
    List<Avis> findAllOrderByCreatedAtDesc();

    /**
     * Récupère les avis d'une formation triés par date décroissante
     */
    @Query(value = "{ 'formationId': ?0, 'valide': true }", sort = "{ 'createdAt': -1 }")
    List<Avis> findValidAvisByFormationIdOrderByCreatedAtDesc(String formationId);

    /**
     * Récupère les avis avec note faible (1-2 étoiles)
     */
    @Query("{ 'formationId': ?0, 'valide': true, 'note': { $lte: 2 } }")
    List<Avis> findNegativeAvisByFormationId(String formationId);

    /**
     * Récupère les avis avec note élevée (4-5 étoiles)
     */
    @Query("{ 'formationId': ?0, 'valide': true, 'note': { $gte: 4 } }")
    List<Avis> findPositiveAvisByFormationId(String formationId);

    /**
     * Récupère les avis récents (derniers 30 jours)
     */
    @Query("{ 'createdAt': { $gte: ?0 }, 'valide': true }")
    List<Avis> findRecentAvis(java.time.LocalDateTime since);

    /**
     * Compte les avis par note pour une formation
     */
    @Query(value = "{ 'formationId': ?0, 'valide': true }", count = true)
    long countValidAvisByFormationId(String formationId);

    /**
     * Récupère la moyenne des notes pour une formation
     */
    @Query(value = "{ 'formationId': ?0, 'valide': true }", fields = "{ 'note': 1 }")
    List<Avis> findNotesByFormationId(String formationId);

    /**
     * Récupère les avis avec commentaire non vide (pour analyse texte)
     */
    @Query("{ 'formationId': ?0, 'valide': true, 'commentaire': { $ne: null, $ne: '' } }")
    List<Avis> findAvisWithCommentaire(String formationId);

    /**
     * Récupère les anomalies (avis où note et sentiment sont incohérents)
     * Note: Cette méthode est indicative, le calcul se fait en Java
     */
    @Query("{ 'valide': true, 'note': { $in: [1, 2, 4, 5] } }")
    List<Avis> findAvisForAnomalyDetection();
}