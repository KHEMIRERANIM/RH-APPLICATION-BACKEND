package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.DocumentFormation;

import java.util.List;

@Repository
public interface DocumentRepository extends MongoRepository<DocumentFormation, String> {
    List<DocumentFormation> findByFormationId(String formationId);
    List<DocumentFormation> findByFormationIdAndType(String formationId, String type);
    List<DocumentFormation> findByFormateurId(String formateurId);
}