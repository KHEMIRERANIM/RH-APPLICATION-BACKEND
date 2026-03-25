package tn.esprit.rh_rse.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Avis;
import tn.esprit.rh_rse.service.AvisService;

import java.util.List;

@RestController
@RequestMapping("/api/avis")
@CrossOrigin(origins = "http://localhost:4200")
public class AvisController {
    @Autowired
    private final AvisService avisService;

    public AvisController(AvisService avisService) {
        this.avisService = avisService;
    }

    @GetMapping
    public ResponseEntity<List<Avis>> getAll() {
        return ResponseEntity.ok(avisService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Avis> getById(@PathVariable String id) {
        return ResponseEntity.ok(avisService.getById(id));
    }

    @GetMapping("/plat/{platId}")
    public ResponseEntity<List<Avis>> getByPlat(@PathVariable String platId) {
        return ResponseEntity.ok(avisService.getByPlat(platId));
    }

    @GetMapping("/plat/{platId}/moyenne")
    public ResponseEntity<Double> getMoyenne(@PathVariable String platId) {
        return ResponseEntity.ok(avisService.getMoyenneNote(platId));
    }

    @PostMapping
    public ResponseEntity<Avis> create(@Valid @RequestBody Avis avis) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(avisService.save(avis));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        avisService.delete(id);
        return ResponseEntity.noContent().build();
    }
}