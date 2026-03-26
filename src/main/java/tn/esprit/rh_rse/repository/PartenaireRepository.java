package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Partenaire;
import tn.esprit.rh_rse.entity.enums.CategorieOffre;

import java.util.List;

public interface PartenaireRepository extends MongoRepository<Partenaire, String> {

    List<Partenaire> findByActifTrue();

    List<Partenaire> findByType(CategorieOffre type);

    boolean existsByNom(String nom);
}