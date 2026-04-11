package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Fidelite;
import tn.esprit.rh_rse.service.FideliteService;

@RestController
@RequestMapping("/api/fidelite")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class FideliteController {

    private final FideliteService fideliteService;

    @GetMapping("/{userId}")
    public ResponseEntity<Fidelite> getFidelite(@PathVariable String userId) {
        return ResponseEntity.ok(fideliteService.getOrCreate(userId));
    }

    @PostMapping("/{userId}/points")
    public ResponseEntity<Fidelite> ajouterPoints(
            @PathVariable String userId,
            @RequestParam double montant) {
        return ResponseEntity.ok(fideliteService.ajouterPoints(userId, montant));
    }

    @PostMapping("/{userId}/utiliser-reduction")
    public ResponseEntity<Fidelite> utiliserReduction(@PathVariable String userId) {
        return ResponseEntity.ok(fideliteService.utiliserReduction(userId));
    }
}
