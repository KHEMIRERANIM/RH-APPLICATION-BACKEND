package tn.esprit.rh_rse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import tn.esprit.rh_rse.dto.request.CareerPlanDTO;
import tn.esprit.rh_rse.entity.Career;
import tn.esprit.rh_rse.entity.EmployeeCertification;
import tn.esprit.rh_rse.entity.EvolutionPlanEntity;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.PlanStatus;
import tn.esprit.rh_rse.repository.CareerRepository;
import tn.esprit.rh_rse.repository.EvolutionPlanRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CareerPlanService {

    private final EvolutionPlanRepository repo;
    private final UserRepository userRepo;
    private final CareerRepository careerRepo;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /** Retourne l'email depuis le JWT (subject = email) */
    private String getCurrentUserEmail() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    /** Récupère l'utilisateur connecté via son email */
    private User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + email));
    }

    // ── CREATE ────────────────────────────────────────────────────────────
    public EvolutionPlanEntity createPlan(CareerPlanDTO dto) {
        User employee = getCurrentUser();

        // poste actuel : on le déduit du champ 'poste' de l'utilisateur
        // si le frontend envoie currentCareerId, on l'utilise
        // sinon on cherche le career dont le title == employee.getPoste()
        String currentCareerId = dto.getCurrentCareerId();
        String currentCareerTitle = null;

        if (currentCareerId != null && !currentCareerId.isEmpty()) {
            Career current = careerRepo.findById(currentCareerId).orElse(null);
            if (current != null) currentCareerTitle = current.getTitle();
        } else if (employee.getPoste() != null) {
            // fallback : cherche par title == poste de l'employé
            Career current = careerRepo.findByTitle(employee.getPoste()).stream()
                    .findFirst().orElse(null);
            if (current != null) {
                currentCareerId    = current.getId();
                currentCareerTitle = current.getTitle();
            }
        }

        Career target = careerRepo.findById(dto.getTargetCareerId())
                .orElseThrow(() -> new RuntimeException("Poste cible introuvable"));

        EvolutionPlanEntity plan = EvolutionPlanEntity.builder()
                .employeeId(employee.getId())          // stocke l'ID Mongo
                .currentCareerId(currentCareerId)
                .targetCareerId(target.getId())
                .status(PlanStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .competencesJson(
                        dto.getCurrentSkills() != null
                                ? dto.getCurrentSkills()
                                : new ArrayList<>()
                )
                .certifications(new ArrayList<>())
                .formationsRecommandees(new ArrayList<>())
                .build();

        return repo.save(plan);
    }

    // ── GET ALL ───────────────────────────────────────────────────────────
    public List<EvolutionPlanEntity> getAll() {
        return repo.findAll();
    }

    // ── GET MY PLANS ──────────────────────────────────────────────────────
    public List<EvolutionPlanEntity> getMyPlans() {
        User employee = getCurrentUser();
        return repo.findByEmployeeId(employee.getId());
    }

    // ── GET BY EMPLOYEE ID ────────────────────────────────────────────────
    public List<EvolutionPlanEntity> getByEmployeeId(String employeeId) {
        return repo.findByEmployeeId(employeeId);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    public EvolutionPlanEntity updatePlan(String id, CareerPlanDTO dto) {
        EvolutionPlanEntity plan = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        if (dto.getCurrentCareerId() != null && !dto.getCurrentCareerId().isEmpty())
            plan.setCurrentCareerId(dto.getCurrentCareerId());

        if (dto.getTargetCareerId() != null && !dto.getTargetCareerId().isEmpty())
            plan.setTargetCareerId(dto.getTargetCareerId());

        if (dto.getCurrentSkills() != null)
            plan.setCompetencesJson(dto.getCurrentSkills());

        plan.setUpdatedAt(LocalDateTime.now());
        return repo.save(plan);
    }

    // ── ENRICH (admin) ────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public EvolutionPlanEntity enrichPlan(String id, Map<String, Object> payload) {
        EvolutionPlanEntity plan = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        if (payload.containsKey("commentaireAdmin"))
            plan.setCommentaireAdmin((String) payload.get("commentaireAdmin"));

        if (payload.containsKey("formationsRecommandees"))
            plan.setFormationsRecommandees(
                    (List<String>) payload.get("formationsRecommandees")
            );

        plan.setUpdatedAt(LocalDateTime.now());
        return repo.save(plan);
    }

    // ── SAVE COMPETENCES ──────────────────────────────────────────────────
    public EvolutionPlanEntity saveCompetences(String id, List<String> competences) {
        EvolutionPlanEntity plan = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        plan.setCompetencesJson(competences);
        plan.setUpdatedAt(LocalDateTime.now());
        return repo.save(plan);
    }

    // ── ADD CERTIFICATION ─────────────────────────────────────────────────
    public EvolutionPlanEntity addCertification(String planId, Object certifObj) {
        EvolutionPlanEntity plan = repo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        EmployeeCertification certif = objectMapper.convertValue(
                certifObj, EmployeeCertification.class
        );
        certif.setId(UUID.randomUUID().toString());
        certif.setEvolutionPlanId(planId);

        if (plan.getCertifications() == null)
            plan.setCertifications(new ArrayList<>());

        plan.getCertifications().add(certif);
        plan.setUpdatedAt(LocalDateTime.now());
        return repo.save(plan);
    }

    // ── UPDATE CERTIFICATION ──────────────────────────────────────────────
    public EvolutionPlanEntity updateCertification(
            String planId, String certifId, Object certifObj) {

        EvolutionPlanEntity plan = repo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        EmployeeCertification updated = objectMapper.convertValue(
                certifObj, EmployeeCertification.class
        );

        List<EmployeeCertification> certifs = plan.getCertifications();
        if (certifs != null) {
            for (int i = 0; i < certifs.size(); i++) {
                if (certifId.equals(certifs.get(i).getId())) {
                    updated.setId(certifId);
                    updated.setEvolutionPlanId(planId);
                    certifs.set(i, updated);
                    break;
                }
            }
        }

        plan.setUpdatedAt(LocalDateTime.now());
        return repo.save(plan);
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    public void deletePlan(String id) {
        repo.deleteById(id);
    }
}