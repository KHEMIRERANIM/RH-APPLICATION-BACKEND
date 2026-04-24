// repository/Formation/VotePropositionRepository.java
package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.VoteProposition;

import java.util.List;
import java.util.Optional;

@Repository
public interface VotePropositionRepository extends MongoRepository<VoteProposition, String> {

    // Trouver tous les votes pour une proposition
    List<VoteProposition> findByPropositionId(String propositionId);

    // ✅ Vérifier si un employé a déjà voté pour une proposition
    boolean existsByPropositionIdAndEmployeId(String propositionId, String employeId);

    // Trouver le vote d'un employé pour une proposition
    Optional<VoteProposition> findByPropositionIdAndEmployeId(String propositionId, String employeId);

    // Compter les votes par type pour une proposition
    long countByPropositionIdAndVote(String propositionId, String vote);

    // Supprimer tous les votes d'une proposition
    void deleteByPropositionId(String propositionId);

    // Trouver les votes par employé
    List<VoteProposition> findByEmployeId(String employeId);
}