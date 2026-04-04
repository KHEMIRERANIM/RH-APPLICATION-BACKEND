package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Bus;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.repository.BusRepository;
import tn.esprit.rh_rse.service.BusService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;

    @Override
    public List<Bus> getAll() {
        return busRepository.findAll();
    }

    @Override
    public Bus getById(String id) {
        return busRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bus non trouvé : " + id));
    }

    @Override
    public Bus create(Bus bus) {
        if (bus.getStatut() == null) {
            bus.setStatut(StatutTrajet.ACTIF);
        }
        if (bus.getPlacesRestantes() == null) {
            bus.setPlacesRestantes(bus.getCapacite());
        }
        bus.setDateCreation(LocalDate.now());
        return busRepository.save(bus);
    }

    @Override
    public Bus update(String id, Bus bus) {
        Bus existing = getById(id);
        existing.setMarque(bus.getMarque());
        existing.setModele(bus.getModele());
        existing.setImmatriculation(bus.getImmatriculation());
        existing.setCapacite(bus.getCapacite());
        existing.setTypeCarburant(bus.getTypeCarburant());
        existing.setLigne(bus.getLigne());
        existing.setHeureDepart(bus.getHeureDepart());
        existing.setDureeMinutes(bus.getDureeMinutes());
        existing.setJoursDisponibles(bus.getJoursDisponibles());
        existing.setPlacesRestantes(bus.getPlacesRestantes());
        existing.setStatut(bus.getStatut());
        existing.setPhotoUrl(bus.getPhotoUrl());  // Ajoutez cette ligne

        return busRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        busRepository.deleteById(id);
    }
}