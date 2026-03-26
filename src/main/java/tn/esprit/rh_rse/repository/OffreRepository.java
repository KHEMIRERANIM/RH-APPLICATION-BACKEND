package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.enums.StatutOffre;
import tn.esprit.rh_rse.entity.enums.TypeContrat;

import java.util.List;

@Repository
public interface OffreRepository extends MongoRepository<Offre, String> {

    List<Offre> findByStatut(StatutOffre statut);

    List<Offre> findByStatutAndDepartement(StatutOffre statut, String departement);

    List<Offre> findByStatutAndTypeContrat(StatutOffre statut, TypeContrat typeContrat);

    List<Offre> findByStatutAndLocalisation(StatutOffre statut, String localisation);

    List<Offre> findByCreateurId(String createurId);

    List<Offre> findByTitreContainingIgnoreCaseAndStatut(String titre, StatutOffre statut);
}