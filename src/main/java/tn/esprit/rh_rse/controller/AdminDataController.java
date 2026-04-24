package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.rh_rse.service.conge.DataGeneratorService;

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