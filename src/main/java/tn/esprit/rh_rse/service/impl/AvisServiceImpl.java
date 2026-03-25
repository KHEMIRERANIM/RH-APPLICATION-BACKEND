package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Avis;
import tn.esprit.rh_rse.repository.AvisRepository;
import tn.esprit.rh_rse.service.AvisService;

import java.util.List;

@Service @RequiredArgsConstructor
public class AvisServiceImpl implements AvisService {

    private final AvisRepository avisRepository;

    @Override
    public List<Avis> getAll() {
        return avisRepository.findAll();
    }

    @Override
    public Avis getById(String id) {
        return avisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé : " + id));
    }

    @Override
    public Avis save(Avis avis) {
        // Un employé ne peut donner qu'un seul avis par plat
        if (avisRepository.existsByUserIdAndPlatId(
                avis.getUserId(), avis.getPlatId())) {
            throw new RuntimeException("Vous avez déjà donné un avis pour ce plat !");
        }
        return avisRepository.save(avis);
    }

    @Override
    public void delete(String id) {
        getById(id);
        avisRepository.deleteById(id);
    }

    @Override
    public List<Avis> getByPlat(String platId) {
        return avisRepository.findByPlatId(platId);
    }

    @Override
    public double getMoyenneNote(String platId) {
        List<Avis> avisList = avisRepository.findByPlatId(platId);
        return avisList.stream()
                .mapToInt(Avis::getNote)
                .average()
                .orElse(0.0);
    }
}