package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import tn.esprit.rh_rse.dto.request.CareerPlanDTO;
import tn.esprit.rh_rse.entity.Career;
import tn.esprit.rh_rse.entity.CareerPlan;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.PlanStatus;
import tn.esprit.rh_rse.repository.CareerRepository;
import tn.esprit.rh_rse.repository.CareerPlanRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerPlanService {

    private final CareerPlanRepository repo;
    private final UserRepository userRepo;
    private final CareerRepository careerRepo;

    private String getCurrentUserId() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    public CareerPlan createPlan(CareerPlanDTO dto) {

        String employeeId = getCurrentUserId();

        User employee = userRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        Career current = careerRepo.findById(dto.getCurrentCareerId())
                .orElseThrow(() -> new RuntimeException("Poste actuel introuvable"));

        Career target = careerRepo.findById(dto.getTargetCareerId())
                .orElseThrow(() -> new RuntimeException("Poste cible introuvable"));

        CareerPlan plan = CareerPlan.builder()
                .employeeId(employeeId)
                .employeeName(employee.getPrenom() + " " + employee.getNom())
                .currentCareerId(current.getId())
                .currentCareerTitle(current.getTitle())
                .targetCareerId(target.getId())
                .targetCareerTitle(target.getTitle())
                .currentSkills(dto.getCurrentSkills())
                .status(PlanStatus.ACTIVE)
                .notes(dto.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return repo.save(plan);
    }

    public List<CareerPlan> getAll() {
        return repo.findAll();
    }

    public List<CareerPlan> getByEmployee(String employeeId) {
        return repo.findByEmployeeId(employeeId);
    }

    public void deletePlan(String id) {
        repo.deleteById(id);
    }
}