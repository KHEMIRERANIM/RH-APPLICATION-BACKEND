package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.OffreAvantage;
import tn.esprit.rh_rse.entity.enums.CategorieOffreAvantage;

import java.time.LocalDate;
import java.util.List;

public interface OffreAvantageRepository extends MongoRepository<OffreAvantage, String> {

    List<OffreAvantage> findByStatutAndDateFinGreaterThanEqual(String statut, LocalDate date);

    List<OffreAvantage> findByCategorieAndStatutAndDateFinGreaterThanEqual(
            CategorieOffreAvantage categorie, String statut, LocalDate date);

    List<OffreAvantage> findByIdPartenaire(String idPartenaire);
}