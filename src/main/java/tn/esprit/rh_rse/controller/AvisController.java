package tn.esprit.rh_rse.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.AvisFormation;
import tn.esprit.rh_rse.service.AvisService;

import java.util.List;

@RestController
@RequestMapping("/api/avis")
@CrossOrigin(origins = "http://localhost:4200")
public class AvisController {
    private final AvisService avisService;

    public AvisController(AvisService avisService) {
        this.avisService = avisService;
    }

    @GetMapping
    public ResponseEntity<List<AvisFormation>> getAll() {
        return ResponseEntity.ok(avisService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvisFormation> getById(@PathVariable String id) {
        return ResponseEntity.ok(avisService.getById(id));
    }

    @GetMapping("/plat/{platId}")
    public ResponseEntity<List<AvisFormation>> getByPlat(@PathVariable String platId) {
        return ResponseEntity.ok(avisService.getByPlat(platId));
    }

    @GetMapping("/plat/{platId}/moyenne")
    public ResponseEntity<Double> getMoyenne(@PathVariable String platId) {
        return ResponseEntity.ok(avisService.getMoyenneNote(platId));
    }

    @PostMapping
    public ResponseEntity<AvisFormation> create(@Valid @RequestBody AvisFormation avisFormation) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(avisService.save(avisFormation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        avisService.delete(id);
        return ResponseEntity.noContent().build();
    }
}