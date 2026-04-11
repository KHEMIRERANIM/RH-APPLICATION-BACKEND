package tn.esprit.rh_rse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.CareerPlanDTO;
import tn.esprit.rh_rse.entity.EvolutionPlanEntity;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.CareerPlanService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evolution_plans")  // ✅ tiret
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CareerPlanController {

    private final CareerPlanService planService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<EvolutionPlanEntity> create(@RequestBody CareerPlanDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.createPlan(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<List<EvolutionPlanEntity>> getMyPlans() {
        String email = SecurityContextHolder
                .getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return ResponseEntity.ok(planService.getByEmployeeId(user.getId()));
    }

    @GetMapping
    public ResponseEntity<List<EvolutionPlanEntity>> getAll() {
        return ResponseEntity.ok(planService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvolutionPlanEntity> update(
            @PathVariable String id,
            @RequestBody CareerPlanDTO dto) {
        return ResponseEntity.ok(planService.updatePlan(id, dto));
    }

    @PatchMapping("/{id}/enrich")
    public ResponseEntity<EvolutionPlanEntity> enrich(
            @PathVariable String id,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(planService.enrichPlan(id, payload));
    }

    // ✅ Reçoit List<Object> (objets Competence) et les convertit en JSON strings
    @PutMapping("/{id}/competences")
    public ResponseEntity<EvolutionPlanEntity> saveCompetences(
            @PathVariable String id,
            @RequestBody List<Object> competences) {
        List<String> competencesStr = competences.stream()
                .map(c -> {
                    try {
                        return objectMapper.writeValueAsString(c);
                    } catch (Exception e) {
                        return c.toString();
                    }
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(planService.saveCompetences(id, competencesStr));
    }

    @PostMapping("/{planId}/certifications")
    public ResponseEntity<EvolutionPlanEntity> addCertification(
            @PathVariable String planId,
            @RequestBody Object certif) {
        return ResponseEntity.ok(planService.addCertification(planId, certif));
    }

    @PutMapping("/{planId}/certifications/{certifId}")
    public ResponseEntity<EvolutionPlanEntity> updateCertification(
            @PathVariable String planId,
            @PathVariable String certifId,
            @RequestBody Object certif) {
        return ResponseEntity.ok(planService.updateCertification(planId, certifId, certif));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}