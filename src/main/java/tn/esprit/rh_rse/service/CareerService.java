package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Career;
import tn.esprit.rh_rse.repository.CareerRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerService {

    private final CareerRepository careerRepository;

    public Career createCareer(Career career) {
        career.setCreatedAt(LocalDateTime.now());
        career.setUpdatedAt(LocalDateTime.now());
        return careerRepository.save(career);
    }

    public List<Career> getAllCareers() {
        return careerRepository.findAll();
    }

    public Career getCareerById(String id) {
        return careerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrière introuvable : " + id));
    }

    public Career updateCareer(String id, Career updated) {
        Career existing = getCareerById(id);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setLevel(updated.getLevel());
        existing.setDomain(updated.getDomain());
        existing.setRequiredSkills(updated.getRequiredSkills());
        existing.setDepartement(updated.getDepartement());
        existing.setPoste(updated.getPoste());
        existing.setSalaryMin(updated.getSalaryMin());
        existing.setSalaryMax(updated.getSalaryMax());
        existing.setIsRemoteFriendly(updated.getIsRemoteFriendly());
        existing.setIsAccessibleForDisabled(updated.getIsAccessibleForDisabled());
        existing.setUserId(updated.getUserId());
        existing.setUpdatedAt(LocalDateTime.now());
        return careerRepository.save(existing);
    }

    public void deleteCareer(String id) {
        if (!careerRepository.existsById(id))
            throw new RuntimeException("Carrière introuvable : " + id);
        careerRepository.deleteById(id);
    }
}