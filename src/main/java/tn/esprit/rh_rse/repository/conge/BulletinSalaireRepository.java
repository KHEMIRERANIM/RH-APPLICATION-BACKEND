package tn.esprit.rh_rse.repository.conge;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.conge.BulletinSalaire;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinSalaireRepository extends MongoRepository<BulletinSalaire, String> {

    List<BulletinSalaire> findByEmployeId(String employeId);

    Optional<BulletinSalaire> findByEmployeIdAndMoisAndAnnee(String employeId, int mois, int annee);

    List<BulletinSalaire> findByEmployeIdAndAnnee(String employeId, int annee);
}
