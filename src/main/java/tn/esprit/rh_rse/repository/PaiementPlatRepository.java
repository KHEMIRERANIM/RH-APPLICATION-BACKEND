package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.PaiementPlat;
import java.util.List;

public interface PaiementPlatRepository extends MongoRepository<PaiementPlat, String> {
    List<PaiementPlat> findByUserId(String userId);
    List<PaiementPlat> findByCommandeId(String commandeId);
    List<PaiementPlat> findByStatut(String statut);
}