package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import tn.esprit.rh_rse.dto.request.CareerPlanDTO;
import tn.esprit.rh_rse.entity.CareerPlan;
import tn.esprit.rh_rse.service.CareerPlanService;

import java.util.List;

@RestController
@RequestMapping("/api/career-plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CareerPlanController {

    private final CareerPlanService planService;

    @PostMapping
    public ResponseEntity<CareerPlan> create(@RequestBody CareerPlanDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.createPlan(dto));
    }

    // ✅ NOUVEAU endpoint propre
    @GetMapping("/me")
    public ResponseEntity<List<CareerPlan>> getMyPlans() {
        String userId = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return ResponseEntity.ok(planService.getByEmployee(userId));
    }

    @GetMapping
    public ResponseEntity<List<CareerPlan>> getAll() {
        return ResponseEntity.ok(planService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}