// ResultatExamenRepository.java
package tn.esprit.rh_rse.repository.Formation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import tn.esprit.rh_rse.entity.Formation.ResultatExamen;

import java.util.List;
import java.util.Optional;

public interface ResultatExamenRepository extends MongoRepository<ResultatExamen, String> {

    List<ResultatExamen> findByExamenId(String examenId);

    List<ResultatExamen> findByExamenIdOrderByNoteDesc(String examenId);

    List<ResultatExamen> findByEmployeId(String employeId);

    Optional<ResultatExamen> findByExamenIdAndEmployeId(String examenId, String employeId);

    boolean existsByExamenIdAndEmployeId(String examenId, String employeId);

    void deleteByExamenId(String examenId);

    // Stats
    @Query(value = "{ 'examen_id': ?0 }", count = true)
    long countByExamenId(String examenId);

    @Query(value = "{ 'examen_id': ?0, 'valide': true }", count = true)
    long countReussisByExamenId(String examenId);
}