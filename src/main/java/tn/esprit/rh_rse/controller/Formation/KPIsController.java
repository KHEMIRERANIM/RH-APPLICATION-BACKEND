package tn.esprit.rh_rse.controller.Formation;
// controller/KPIsController.java


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.Formation.KPIsDTO;
import tn.esprit.rh_rse.service.Formation.KPIsService;

@RestController
@RequestMapping("/api/kpis")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class KPIsController {

    private final KPIsService kpisService;

    @GetMapping("/dashboard")
    public ResponseEntity<KPIsDTO> getDashboardKPIs() {
        return ResponseEntity.ok(kpisService.calculerTousLesKPIs());
    }

    @GetMapping("/risque/{formationId}")
    public ResponseEntity<Double> getRisqueFormation(@PathVariable String formationId) {
        // Tu devras déplacer calculerRisquePourFormation dans un service public
        return ResponseEntity.ok(0.0);
    }
}