package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import tn.esprit.rh_rse.entity.Formation.Formation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FormationRepository extends MongoRepository<Formation, String> {

    List<Formation> findByActiveTrue();

    @Query("SELECT f FROM Formation f WHERE f.active = true AND f.dateDebut >= now")
    List<Formation> findFormationsDisponibles(LocalDateTime now);
    List<Formation> findByDateFinBefore(LocalDateTime date);

    List<Formation> findByType(String type);

    @Query("{ $or: [ { 'titre': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } }, { 'formateur': { $regex: ?0, $options: 'i' } } ] }")
    List<Formation> searchFormations(String keyword);

    // === NOUVEAU : Trouver les formations qui ont un prérequis spécifique ===
    @Query("{ 'prerequisFormationId': ?0 }")
    List<Formation> findByPrerequisFormationId(String prerequisFormationId);
    // FormationRepository.java - Ajouter cette méthode
    default void updateNoteMoyenne(String formationId, Double noteMoyenne) {
        Optional<Formation> formationOpt = findById(formationId);
        if (formationOpt.isPresent()) {
            Formation formation = formationOpt.get();
            formation.setNoteMoyenne(noteMoyenne);
            save(formation);
        }
    }

    List<Formation> findByFormateurId(String formateurId);
}