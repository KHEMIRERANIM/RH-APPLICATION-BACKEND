// ExamenRepository.java
package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.Examen;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamenRepository extends MongoRepository<Examen, String> {




    List<Examen> findByFormationId(String formationId);

    // ✅ Si vous voulez trier, faites comme ceci
    List<Examen> findByFormationIdOrderByDateLimiteAsc(String formationId);

    // Autres méthodes utiles
    List<Examen> findByDateLimiteBefore(LocalDateTime date);

    List<Examen> findByDateLimiteAfter(LocalDateTime date);
}