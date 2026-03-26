package tn.esprit.rh_rse.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Vehicule;
import tn.esprit.rh_rse.repository.VehiculeRepository;
import tn.esprit.rh_rse.service.VehiculeService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiculeServiceImpl implements VehiculeService {

    private final VehiculeRepository vehiculeRepository;

    @Override
    public List<Vehicule> getAll() {
        return vehiculeRepository.findAll();
    }

    @Override
    public Vehicule getById(String id) {
        return vehiculeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicule non trouvé : " + id));
    }

    @Override
    public Vehicule create(Vehicule vehicule) {
        return vehiculeRepository.save(vehicule);
    }

    @Override
    public Vehicule update(String id, Vehicule vehicule) {
        Vehicule existing = getById(id);
        existing.setMarque(vehicule.getMarque());
        existing.setModele(vehicule.getModele());
        existing.setImmatriculation(vehicule.getImmatriculation());
        existing.setNbPlaces(vehicule.getNbPlaces());
        existing.setTypeCarburant(vehicule.getTypeCarburant());
        return vehiculeRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        vehiculeRepository.deleteById(id);
    }
}
