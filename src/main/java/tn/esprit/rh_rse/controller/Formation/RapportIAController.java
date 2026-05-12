// controller/Formation/RapportIAController.java
package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.rh_rse.dto.Formation.KPIsDTO;
import tn.esprit.rh_rse.dto.Formation.RapportIADTO;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;
import tn.esprit.rh_rse.service.Formation.DeepSeekService;
import tn.esprit.rh_rse.service.Formation.KPIsService;

import java.util.List;

@RestController
@RequestMapping("/api/rapport-ia")
@CrossOrigin(origins = "http://localhost:4200",allowCredentials = "true")
@RequiredArgsConstructor
public class RapportIAController {

    private final KPIsService kpisService;
    private final FormationRepository formationRepository;
    private final DeepSeekService deepSeekService;

    @GetMapping("/generer")
    public ResponseEntity<RapportIADTO> genererRapport() {
        KPIsDTO kpis = kpisService.calculerTousLesKPIs();
        List<Formation> formations = formationRepository.findAll();

        RapportIADTO rapport = deepSeekService.genererRapportComplet(kpis, formations);

        return ResponseEntity.ok(rapport);
    }
}