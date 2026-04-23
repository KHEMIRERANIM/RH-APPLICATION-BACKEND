package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.response.PredictionResponse;
import tn.esprit.rh_rse.dto.response.RecommandationEmployeResponse;
import tn.esprit.rh_rse.service.PredictionService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping("/admin/charge")
    public ResponseEntity<PredictionResponse> predireCharge(
            @RequestParam(defaultValue = "0") int mois,
            @RequestParam(defaultValue = "0") int annee) {

        int moisActuel = mois > 0 ? mois : LocalDate.now().getMonthValue();
        int anneeActuelle = annee > 0 ? annee : LocalDate.now().getYear();

        PredictionResponse response = predictionService.predireChargeMois(moisActuel, anneeActuelle);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/tendances")
    public ResponseEntity<List<PredictionResponse>> predireTendances() {
        return ResponseEntity.ok(predictionService.predireTendances());
    }

    @GetMapping("/employee/{employeId}/recommandations")
    public ResponseEntity<List<RecommandationEmployeResponse>> getRecommandations(
            @PathVariable String employeId) {
        return ResponseEntity.ok(predictionService.getRecommandationsEmploye(employeId));
    }
}