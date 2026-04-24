package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Formation.RenduExercice;

import java.util.List;

@Repository
public interface RenduExerciceRepository extends MongoRepository<RenduExercice, String> {
    List<RenduExercice> findByEmployeId(String employeId);
    List<RenduExercice> findByDocumentId(String documentId);
    RenduExercice findByDocumentIdAndEmployeId(String documentId, String employeId);
    List<RenduExercice> findByDocumentIdAndStatut(String documentId, String statut);
}