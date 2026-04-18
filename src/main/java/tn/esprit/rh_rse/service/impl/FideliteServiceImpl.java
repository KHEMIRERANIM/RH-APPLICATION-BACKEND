package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Fidelite;
import tn.esprit.rh_rse.repository.FideliteRepository;
import tn.esprit.rh_rse.service.FideliteService;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FideliteServiceImpl implements FideliteService {

    private final FideliteRepository fideliteRepository;

    @Override
    public Fidelite getOrCreate(String userId) {
        return fideliteRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Fidelite f = new Fidelite();
                    f.setUserId(userId);
                    return fideliteRepository.save(f);
                });
    }

    @Override
    public Fidelite ajouterPoints(String userId, double montant) {
        Fidelite f = getOrCreate(userId);
        int pointsGagnes = (int)(montant * Fidelite.POINTS_PAR_TND);
        f.setPoints(f.getPoints() + pointsGagnes);
        f.setTotalDepense(f.getTotalDepense() + montant);
        f.getHistorique().add("+" + pointsGagnes + " pts le " + LocalDate.now()
                + " (" + montant + " TND)");
        if (f.getPoints() >= Fidelite.SEUIL_REDUCTION) {
            f.setReductionDisponible(true);
        }
        return fideliteRepository.save(f);
    }

    @Override
    public Fidelite utiliserReduction(String userId) {
        Fidelite f = getOrCreate(userId);
        if (!f.isReductionDisponible()) {
            throw new RuntimeException("Aucune reduction disponible");
        }
        f.setPoints(f.getPoints() - Fidelite.SEUIL_REDUCTION);
        f.setReductionDisponible(f.getPoints() >= Fidelite.SEUIL_REDUCTION);
        f.getHistorique().add("Reduction de " + f.getMontantReduction()
                + " TND utilisee le " + LocalDate.now());
        return fideliteRepository.save(f);
    }

    @Override
    public List<Fidelite> getAll() {
        return fideliteRepository.findAll();
    }
}