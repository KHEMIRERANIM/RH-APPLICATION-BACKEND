// repository/Formation/FormationPropositionRepository.java
package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.FormationProposition;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FormationPropositionRepository extends MongoRepository<FormationProposition, String> {

    // ==================== RECHERCHES PAR STATUT ====================

    // Recherche par statut simple
    List<FormationProposition> findByStatut(String statut);

    // ✅ Recherche par statut et statutVote
    List<FormationProposition> findByStatutAndStatutVote(String statut, String statutVote);

    // Recherche par technologie
    List<FormationProposition> findByTechnologie(String technologie);

    // Recherche par source (IA_RECOMMENDATION, ADMIN_MANUAL)
    List<FormationProposition> findBySource(String source);

    // Recherche par score minimum
    List<FormationProposition> findByScoreIAGreaterThanEqual(Double score);

    // ==================== COMPTAGES ====================

    // Comptage par statut
    long countByStatut(String statut);

    // Comptage par statut et statutVote
    long countByStatutAndStatutVote(String statut, String statutVote);

    // ==================== VÉRIFICATIONS ====================

    // Vérifier si une proposition existe déjà pour une technologie
    boolean existsByTechnologieAndStatutIn(String technologie, List<String> statuts);

    // ==================== RECHERCHES AVEC CRITÈRES MULTIPLES ====================

    // Recherche avec critères multiples
    @Query("{ 'statut': ?0, 'scoreIA': { $gte: ?1 } }")
    List<FormationProposition> findByStatutAndMinScore(String statut, Double minScore);

    // Recherche des propositions avec plus de X employés intéressés
    @Query("{ 'employesInteresses': { $size: { $gte: ?0 } } }")
    List<FormationProposition> findByMinimumInterets(int minInterets);

    // Recherche par période
    List<FormationProposition> findByDatePropositionBetween(LocalDateTime debut, LocalDateTime fin);

    // Top propositions par nombre d'intérêts
    @Query(value = "{ 'statut': ?0 }", sort = "{ 'employesInteresses': -1 }")
    List<FormationProposition> findTopByStatutOrderByInteretsDesc(String statut, org.springframework.data.domain.Pageable pageable);

    // Supprimer les propositions rejetées anciennes
    void deleteByStatutAndDatePropositionBefore(String statut, LocalDateTime date);

    // ==================== RECHERCHES POUR VOTE ====================

    // Rechercher les propositions ouvertes au vote
    @Query("{ 'statut': 'VALIDEE', 'statutVote': 'OUVERT' }")
    List<FormationProposition> findPropositionsOuvertesVote();

    // Rechercher les propositions avec vote clos
    @Query("{ 'statut': 'VALIDEE', 'statutVote': 'CLOTURE' }")
    List<FormationProposition> findPropositionsVoteClos();

    // Rechercher les propositions en attente de vote
    @Query("{ 'statut': 'VALIDEE', 'statutVote': { $exists: false } }")
    List<FormationProposition> findPropositionsSansVote();

    // Rechercher par statutVote
    List<FormationProposition> findByStatutVote(String statutVote);

    // Rechercher par statutVote et statut
    List<FormationProposition> findByStatutVoteAndStatut(String statutVote, String statut);
}