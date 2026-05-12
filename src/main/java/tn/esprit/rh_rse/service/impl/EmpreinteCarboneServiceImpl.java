package tn.esprit.rh_rse.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.EmpreinteCarbone;
import tn.esprit.rh_rse.entity.enums.RoleTrajet;
import tn.esprit.rh_rse.entity.enums.TypeCarburant;
import tn.esprit.rh_rse.repository.EmpreinteCarboneRepository;
import tn.esprit.rh_rse.service.EmpreinteCarboneService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpreinteCarboneServiceImpl implements EmpreinteCarboneService {

    private final EmpreinteCarboneRepository empreinteCarboneRepository;

    @Override
    public List<EmpreinteCarbone> getAll() {
        return empreinteCarboneRepository.findAll();
    }

    @Override
    public EmpreinteCarbone getById(String id) {
        return empreinteCarboneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("EmpreinteCarbone non trouvée : " + id));
    }

    @Override
    public EmpreinteCarbone create(EmpreinteCarbone empreinte) {
        // Calcul CO2 selon type carburant
        double facteurCo2 = getFacteurCo2(empreinte.getTypeCarburant());

        // CO2 si voiture solo
        double co2Solo = empreinte.getDistanceKm() * facteurCo2;

        // CO2 économisé selon rôle
        double co2Economise;
        if (empreinte.getRole() == RoleTrajet.CONDUCTEUR) {
            // Conducteur partage le CO2 avec les passagers
            co2Economise = co2Solo - (co2Solo / (empreinte.getNbPassagers() + 1));
        } else {
            // Passager économise tout son CO2 solo
            co2Economise = co2Solo / empreinte.getNbPassagers();
        }

        // Points éco : 1 point par 0.1 kg économisé
        int points = (int) (co2Economise * 10);

        empreinte.setCo2EconomiseKg(Math.round(co2Economise * 100.0) / 100.0);
        empreinte.setPointsEco(points);
        empreinte.setDateCalcul(LocalDate.now());

        return empreinteCarboneRepository.save(empreinte);
    }

    @Override
    public EmpreinteCarbone update(String id, EmpreinteCarbone empreinte) {
        EmpreinteCarbone existing = getById(id);
        existing.setDistanceKm(empreinte.getDistanceKm());
        existing.setNbPassagers(empreinte.getNbPassagers());
        existing.setTypeCarburant(empreinte.getTypeCarburant());
        existing.setRole(empreinte.getRole());
        return empreinteCarboneRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        empreinteCarboneRepository.deleteById(id);
    }

    @Override
    public List<EmpreinteCarbone> getByEmployeId(String employeId) {
        return empreinteCarboneRepository.findByEmployeId(employeId);
    }

    @Override
    public List<EmpreinteCarbone> getByTrajetId(String trajetId) {
        return empreinteCarboneRepository.findByTrajetId(trajetId);
    }

    @Override
    public int getTotalPointsByEmploye(String employeId) {
        return empreinteCarboneRepository.findByEmployeId(employeId)
                .stream()
                .mapToInt(e -> e.getPointsEco() != null ? e.getPointsEco() : 0)
                .sum();
    }

    // ─── Facteur CO2 selon carburant ───────────────────────
    private double getFacteurCo2(TypeCarburant type) {
        switch (type) {
            case ESSENCE:    return 0.21;
            case DIESEL:     return 0.17;
            case HYBRIDE:    return 0.10;
            case ELECTRIQUE: return 0.05;
            default:         return 0.21;
        }
    }
}