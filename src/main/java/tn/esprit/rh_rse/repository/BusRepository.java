package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Bus;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import java.util.List;

public interface BusRepository extends MongoRepository<Bus, String> {
    List<Bus> findByStatut(StatutTrajet statut);
    List<Bus> findByStatutAndPlacesRestantesGreaterThan(
            StatutTrajet statut, int places);
    List<Bus> findByPlacesRestantesGreaterThan(int places);

    List<Bus> findByPackId(String packId);

    List<Bus> findByPackIdAndStatut(String packId, StatutTrajet statut);
}