package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.AvisFormation;
import tn.esprit.rh_rse.repository.AvisFormationRepository;
import tn.esprit.rh_rse.service.AvisService;

import java.util.List;

@Service @RequiredArgsConstructor
public class AvisServiceImpl implements AvisService {

    private final AvisFormationRepository avisFormationRepository;

    @Override
    public List<AvisFormation> getAll() {
        return avisFormationRepository.findAll();
    }

    @Override
    public AvisFormation getById(String id) {
        return avisFormationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé : " + id));
    }

    @Override
    public AvisFormation save(AvisFormation avisFormation) {
        // Un employé ne peut donner qu'un seul avis par plat
        if (avisFormationRepository.existsByUserIdAndPlatId(
                avisFormation.getUserId(), avisFormation.getPlatId())) {
            throw new RuntimeException("Vous avez déjà donné un avis pour ce plat !");
        }
        return avisFormationRepository.save(avisFormation);
    }

    @Override
    public void delete(String id) {
        getById(id);
        avisFormationRepository.deleteById(id);
    }

    @Override
    public List<AvisFormation> getByPlat(String platId) {
        return avisFormationRepository.findByPlatId(platId);
    }

    @Override
    public double getMoyenneNote(String platId) {
        List<AvisFormation> avisFormationList = avisFormationRepository.findByPlatId(platId);
        return avisFormationList.stream()
                .mapToInt(AvisFormation::getNote)
                .average()
                .orElse(0.0);
    }
}