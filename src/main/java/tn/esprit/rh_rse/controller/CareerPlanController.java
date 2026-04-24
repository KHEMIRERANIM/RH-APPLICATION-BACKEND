package tn.esprit.rh_rse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.CareerPlanDTO;
import tn.esprit.rh_rse.entity.EvolutionPlanEntity;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.CareerPlanService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evolution_plans")
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
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
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

    @PostMapping("/{planId}/certifications/{certifId}/upload")
    public ResponseEntity<Map<String, String>> uploadCertificationFile(
            @PathVariable String planId,
            @PathVariable String certifId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le fichier est vide"));
        }

        try {
            Map<String, String> result = planService.uploadCertificationFile(planId, certifId, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'upload : " + e.getMessage()));
        }
    }

    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("uploads/certificats").toAbsolutePath().normalize().resolve(fileName).normalize();

            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}