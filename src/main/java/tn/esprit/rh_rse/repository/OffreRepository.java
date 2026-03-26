package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.enums.CategorieOffre;

import java.time.LocalDate;
import java.util.List;

public interface OffreRepository extends MongoRepository<Offre, String> {

    List<Offre> findByStatutAndDateFinGreaterThanEqual(String statut, LocalDate date);

    List<Offre> findByCategorieAndStatutAndDateFinGreaterThanEqual(
            CategorieOffre categorie, String statut, LocalDate date);

    List<Offre> findByIdPartenaire(String idPartenaire);
}