package tn.esprit.rh_rse.controller.conge;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.response.conge.PredictionResponse;
import tn.esprit.rh_rse.dto.response.conge.RecommandationEmployeResponse;
import tn.esprit.rh_rse.service.conge.PredictionCongeService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PredictionCongeController {

    private final PredictionCongeService predictionCongeService;

    @GetMapping("/admin/charge")
    public ResponseEntity<PredictionResponse> predireCharge(
            @RequestParam(defaultValue = "0") int mois,
            @RequestParam(defaultValue = "0") int annee) {

        int moisActuel = mois > 0 ? mois : LocalDate.now().getMonthValue();
        int anneeActuelle = annee > 0 ? annee : LocalDate.now().getYear();

        PredictionResponse response = predictionCongeService.predireChargeMois(moisActuel, anneeActuelle);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/tendances")
    public ResponseEntity<List<PredictionResponse>> predireTendances() {
        return ResponseEntity.ok(predictionCongeService.predireTendances());
    }

    @GetMapping("/employee/{employeId}/recommandations")
    public ResponseEntity<List<RecommandationEmployeResponse>> getRecommandations(
            @PathVariable String employeId) {
        return ResponseEntity.ok(predictionCongeService.getRecommandationsEmploye(employeId));
    }
}