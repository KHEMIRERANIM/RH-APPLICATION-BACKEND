package tn.esprit.rh_rse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.CareerPlanDTO;
import tn.esprit.rh_rse.entity.*;
import tn.esprit.rh_rse.entity.enums.CertificationStatus;
import tn.esprit.rh_rse.entity.enums.PlanStatus;
import tn.esprit.rh_rse.repository.CareerRepository;
import tn.esprit.rh_rse.repository.EvolutionPlanRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CareerPlanService {

    private final EvolutionPlanRepository repo;
    private final UserRepository userRepo;
    private final CareerRepository careerRepo;
    private final NotifCarriereService notificationService;

    @Value("${app.upload.dir:uploads/certificats}")
    private String uploadDir;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + email));
    }

    // ── CALCUL DES SCORES BACKEND ─────────────────────────────────────────
    private void computeScores(EvolutionPlanEntity plan) {
        if (plan == null) return;

        List<EmployeeCertification> certifs = plan.getCertifications() != null
                ? plan.getCertifications()
                : new ArrayList<>();

        long totalTech = certifs.stream()
                .filter(c -> c.getType() != null && c.getType().name().equalsIgnoreCase("TECHNIQUE"))
                .count();

        long obtainedTech = certifs.stream()
                .filter(c -> c.getType() != null
                        && c.getType().name().equalsIgnoreCase("TECHNIQUE")
                        && c.getStatut() != null
                        && c.getStatut().name().equalsIgnoreCase("OBTENU"))
                .count();

        long totalSoft = certifs.stream()
                .filter(c -> c.getType() != null && c.getType().name().equalsIgnoreCase("SOFT_SKILL"))
                .count();

        long obtainedSoft = certifs.stream()
                .filter(c -> c.getType() != null
                        && c.getType().name().equalsIgnoreCase("SOFT_SKILL")
                        && c.getStatut() != null
                        && c.getStatut().name().equalsIgnoreCase("OBTENU"))
                .count();

        int scoreTechnique = totalTech == 0 ? 0 : (int) Math.round((obtainedTech * 100.0) / totalTech);
        int scoreSoftSkill = totalSoft == 0 ? 0 : (int) Math.round((obtainedSoft * 100.0) / totalSoft);

        int poidsTech = plan.getPoidsScoreTechnique() != null ? plan.getPoidsScoreTechnique() : 70;
        int poidsSoft = plan.getPoidsScoreSoftSkill() != null ? plan.getPoidsScoreSoftSkill() : 30;

        int scoreGlobal = (int) Math.round(
                (scoreTechnique * poidsTech + scoreSoftSkill * poidsSoft) / 100.0
        );

        plan.setScoreTechnique(scoreTechnique);
        plan.setScoreSoftSkill(scoreSoftSkill);
        plan.setScoreGlobal(scoreGlobal);
    }

    // ── CREATE ────────────────────────────────────────────────────────────
    public EvolutionPlanEntity createPlan(CareerPlanDTO dto) {
        User employee = getCurrentUser();

        String currentCareerId = dto.getCurrentCareerId();
        if (currentCareerId == null && employee.getPoste() != null) {
            Career current = careerRepo.findByTitle(employee.getPoste())
                    .stream().findFirst().orElse(null);
            if (current != null) currentCareerId = current.getId();
        }

        Career target = careerRepo.findById(dto.getTargetCareerId())
                .orElseThrow(() -> new RuntimeException("Poste cible introuvable"));

        // Copier les certifications requises du poste cible
        List<EmployeeCertification> certifications = new ArrayList<>();
        if (target.getCertifRequises() != null) {
            for (EmployeeCertification certifRequise : target.getCertifRequises()) {
                EmployeeCertification certif = new EmployeeCertification();
                certif.setId(UUID.randomUUID().toString());
                certif.setNom(certifRequise.getNom());
                certif.setType(certifRequise.getType());
                certif.setNiveauRequis(certifRequise.getNiveauRequis());
                certif.setObligatoire(true);
                certif.setStatut(CertificationStatus.NON_COMMENCE);
                certif.setMethodeEval(certifRequise.getMethodeEval());
                certif.setValideParAdmin(false);
                certifications.add(certif);
            }
        }

        EvolutionPlanEntity plan = EvolutionPlanEntity.builder()
                .employeeId(employee.getId())
                .currentCareerId(currentCareerId)
                .targetCareerId(target.getId())
                .status(PlanStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .competencesJson(dto.getCurrentSkills() != null
                        ? dto.getCurrentSkills() : new ArrayList<>())
                .certifications(certifications)
                .formationsRecommandees(new ArrayList<>())
                .commentaireAdmin(null)
                .poidsScoreTechnique(70)
                .poidsScoreSoftSkill(30)
                .scoreTechnique(0)
                .scoreSoftSkill(0)
                .scoreGlobal(0)
                .build();

        computeScores(plan);
        return repo.save(plan);
    }

    // ── GET ALL ───────────────────────────────────────────────────────────
    public List<EvolutionPlanEntity> getAll() {
        List<EvolutionPlanEntity> plans = repo.findAll();
        plans.forEach(this::computeScores);
        return plans;
    }

    public List<EvolutionPlanEntity> getMyPlans() {
        List<EvolutionPlanEntity> plans = repo.findByEmployeeId(getCurrentUser().getId());
        plans.forEach(this::computeScores);
        return plans;
    }

    public List<EvolutionPlanEntity> getByEmployeeId(String employeeId) {
        List<EvolutionPlanEntity> plans = repo.findByEmployeeId(employeeId);
        plans.forEach(this::computeScores);
        return plans;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    public EvolutionPlanEntity updatePlan(String id, CareerPlanDTO dto) {
        EvolutionPlanEntity plan = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        if (dto.getCurrentCareerId() != null) plan.setCurrentCareerId(dto.getCurrentCareerId());
        if (dto.getTargetCareerId() != null) plan.setTargetCareerId(dto.getTargetCareerId());
        if (dto.getCurrentSkills() != null) plan.setCompetencesJson(dto.getCurrentSkills());

        plan.setUpdatedAt(LocalDateTime.now());
        computeScores(plan);
        return repo.save(plan);
    }

    // ── ENRICH (admin) ────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public EvolutionPlanEntity enrichPlan(String id, Map<String, Object> payload) {
        EvolutionPlanEntity plan = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        if (payload.containsKey("commentaireAdmin")) {
            plan.setCommentaireAdmin((String) payload.get("commentaireAdmin"));
        }

        if (payload.containsKey("formationsRecommandees")) {
            plan.setFormationsRecommandees((List<String>) payload.get("formationsRecommandees"));
        }

        plan.setUpdatedAt(LocalDateTime.now());
        computeScores(plan);
        return repo.save(plan);
    }

    // ── SAVE COMPETENCES ──────────────────────────────────────────────────
    public EvolutionPlanEntity saveCompetences(String id, List<String> competences) {
        EvolutionPlanEntity plan = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        plan.setCompetencesJson(competences);
        plan.setUpdatedAt(LocalDateTime.now());
        computeScores(plan);
        return repo.save(plan);
    }

    // ── ADD CERTIFICATION ─────────────────────────────────────────────────
    public EvolutionPlanEntity addCertification(String planId, Object certifObj) {
        EvolutionPlanEntity plan = repo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        EmployeeCertification certif = objectMapper.convertValue(certifObj, EmployeeCertification.class);
        certif.setId(UUID.randomUUID().toString());
        certif.setEvolutionPlanId(planId);

        if (plan.getCertifications() == null) {
            plan.setCertifications(new ArrayList<>());
        }

        plan.getCertifications().add(certif);
        plan.setUpdatedAt(LocalDateTime.now());
        computeScores(plan);
        return repo.save(plan);
    }

    // ── UPDATE CERTIFICATION ──────────────────────────────────────────────
    public EvolutionPlanEntity updateCertification(String planId, String certifId, Object certifObj) {
        EvolutionPlanEntity plan = repo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        EmployeeCertification updated = objectMapper.convertValue(certifObj, EmployeeCertification.class);
        List<EmployeeCertification> certifs = plan.getCertifications();

        if (certifs != null) {
            for (int i = 0; i < certifs.size(); i++) {
                if (certifId.equals(certifs.get(i).getId())) {
                    updated.setId(certifId);
                    updated.setEvolutionPlanId(planId);

                    EmployeeCertification existing = certifs.get(i);

                    // Préserver les champs non envoyés
                    if (updated.getFichierUrl() == null) updated.setFichierUrl(existing.getFichierUrl());
                    if (updated.getFichierNom() == null) updated.setFichierNom(existing.getFichierNom());
                    if (updated.getNom() == null) updated.setNom(existing.getNom());
                    if (updated.getType() == null) updated.setType(existing.getType());
                    if (updated.getMethodeEval() == null) updated.setMethodeEval(existing.getMethodeEval());
                    if (updated.getStatut() == null) updated.setStatut(existing.getStatut());
                    if (updated.getNiveauRequis() == null) updated.setNiveauRequis(existing.getNiveauRequis());
                    if (updated.getCommentaireAdmin() == null) updated.setCommentaireAdmin(existing.getCommentaireAdmin());
                    if (updated.getObligatoire() == null) updated.setObligatoire(existing.getObligatoire());
                    if (updated.getValideParAdmin() == null) updated.setValideParAdmin(existing.getValideParAdmin());

                    // Validation admin → statut OBTENU + notification
                    if (Boolean.TRUE.equals(updated.getValideParAdmin())
                            && !Boolean.TRUE.equals(existing.getValideParAdmin())) {
                        updated.setStatut(CertificationStatus.OBTENU);
                        String nomCertif = updated.getNom() != null ? updated.getNom() : "certification";

                        notificationService.send(
                                plan.getEmployeeId(),
                                "✅ Certification validée !",
                                "Votre certification \"" + nomCertif + "\" a été validée par l'administrateur.",
                                "CERTIF_VALIDATED"
                        );
                    }

                    // Rejet admin → statut EN_COURS
                    if (Boolean.FALSE.equals(updated.getValideParAdmin())
                            && Boolean.TRUE.equals(existing.getValideParAdmin())) {
                        updated.setStatut(CertificationStatus.EN_COURS);
                    }

                    certifs.set(i, updated);
                    break;
                }
            }
        }

        plan.setUpdatedAt(LocalDateTime.now());
        computeScores(plan);
        return repo.save(plan);
    }

    // ── UPLOAD FICHIER POUR CERTIFICATION ─────────────────────────────────
    public Map<String, String> uploadCertificationFile(
            String planId,
            String certifId,
            MultipartFile file) throws IOException {

        EvolutionPlanEntity plan = repo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan introuvable"));

        List<EmployeeCertification> certifs = plan.getCertifications();
        if (certifs == null) {
            throw new RuntimeException("Aucune certification trouvée dans ce plan");
        }

        EmployeeCertification target = certifs.stream()
                .filter(c -> c.getId().equals(certifId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Certification introuvable"));

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = file.getOriginalFilename();
        String extension = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".pdf";

        String uniqueName = "certif_" + certifId + "_" + System.currentTimeMillis() + extension;

        Path filePath = uploadPath.resolve(uniqueName);
        Files.write(filePath, file.getBytes());

        target.setFichierNom(originalName);
        target.setFichierUrl("/api/evolution_plans/files/" + uniqueName);

        if (CertificationStatus.NON_COMMENCE.equals(target.getStatut())) {
            target.setStatut(CertificationStatus.EN_COURS);
        }

        plan.setUpdatedAt(LocalDateTime.now());
        computeScores(plan);
        repo.save(plan);

        return Map.of(
                "fileName", originalName != null ? originalName : uniqueName,
                "fileUrl", "/api/evolution_plans/files/" + uniqueName,
                "message", "Fichier uploadé avec succès"
        );
    }

    // ── DELETE PLAN ───────────────────────────────────────────────────────
    public void deletePlan(String id) {
        repo.deleteById(id);
    }
}