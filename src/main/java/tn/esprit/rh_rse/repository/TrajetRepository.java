package tn.esprit.rh_rse.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Trajet;
import tn.esprit.rh_rse.entity.enums.CategorieTransport;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;

import java.util.List;

@Repository
public interface TrajetRepository extends MongoRepository<Trajet, String> {
    List<Trajet> findByStatut(StatutTrajet statut);
    List<Trajet> findByCategorie(CategorieTransport categorie);
    List<Trajet> findByEmployeId(String employeId);
}