package tn.esprit.rh_rse.service.conge;

import tn.esprit.rh_rse.dto.request.conge.BulletinSalaireRequest;
import tn.esprit.rh_rse.dto.response.conge.BulletinSalaireResponse;

import java.util.List;

public interface SalaireService {

    BulletinSalaireResponse creerBulletin(BulletinSalaireRequest request);

    List<BulletinSalaireResponse> getBulletinsByEmploye(String employeId);

    BulletinSalaireResponse getBulletinById(String id);

    BulletinSalaireResponse getBulletinByMois(String employeId, int mois, int annee);

    List<BulletinSalaireResponse> getBulletinsByAnnee(String employeId, int annee);

    BulletinSalaireResponse modifierBulletin(String id, BulletinSalaireRequest request);

    void supprimerBulletin(String id);
    List<BulletinSalaireResponse> getAllBulletins();
}
