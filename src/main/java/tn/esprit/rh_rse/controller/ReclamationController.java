package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Reclamation;
import tn.esprit.rh_rse.service.ReclamationService;

import java.util.List;

@RestController
@RequestMapping("/api/reclamations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReclamationController {

    private final ReclamationService reclamationService;

    @PostMapping
    public Reclamation saveReclamation(@RequestBody Reclamation reclamation) {
        return reclamationService.saveReclamation(reclamation);
    }

    @GetMapping
    public List<Reclamation> getAllReclamations() {
        return reclamationService.getAllReclamations();
    }
}
