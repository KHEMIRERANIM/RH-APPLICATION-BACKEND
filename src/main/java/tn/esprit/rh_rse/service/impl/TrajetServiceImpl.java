package tn.esprit.rh_rse.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Trajet;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.repository.TrajetRepository;
import tn.esprit.rh_rse.service.TrajetService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrajetServiceImpl implements TrajetService {

    private final TrajetRepository trajetRepository;

    @Override
    public List<Trajet> getAll() {
        return trajetRepository.findAll();
    }

    @Override
    public Trajet getById(String id) {
        return trajetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trajet non trouvé : " + id));
    }

    @Override
    public Trajet create(Trajet trajet) {
        trajet.setDateCreation(LocalDate.now());
        trajet.setPlacesRestantes(trajet.getPlacesDisponibles());
        trajet.setStatut(StatutTrajet.ACTIF);
        return trajetRepository.save(trajet);
    }

    @Override
    public Trajet update(String id, Trajet trajet) {
        Trajet existing = getById(id);
        existing.setAdresseDepart(trajet.getAdresseDepart());
        existing.setAdresseArrivee(trajet.getAdresseArrivee());
        existing.setHeureDepart(trajet.getHeureDepart());
        existing.setJoursDisponibles(trajet.getJoursDisponibles());
        existing.setPlacesDisponibles(trajet.getPlacesDisponibles());
        existing.setCategorie(trajet.getCategorie());
        existing.setStatut(trajet.getStatut());
        existing.setVehiculeId(trajet.getVehiculeId());
        return trajetRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        trajetRepository.deleteById(id);
    }

    @Override
    public List<Trajet> getByStatut(StatutTrajet statut) {
        return trajetRepository.findByStatut(statut);
    }
}