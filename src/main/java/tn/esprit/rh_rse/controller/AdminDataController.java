package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.service.DataGeneratorService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AdminDataController {

    private final DataGeneratorService dataGeneratorService;

    @PostMapping("/generer-donnees")
    public ResponseEntity<String> genererDonneesSynthetiques() {
        String resultat = dataGeneratorService.genererDonneesSynthetiques();
        return ResponseEntity.ok(resultat);
    }
}