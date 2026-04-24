package tn.esprit.rh_rse.controller.Formation;
// controller/Formation/FraudController.java

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.Formation.FraudEventDTO;
import tn.esprit.rh_rse.dto.Formation.FraudResponseDTO;
import tn.esprit.rh_rse.dto.Formation.FraudSummaryDTO;
import tn.esprit.rh_rse.service.Formation.FraudService;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudController {

    private final FraudService fraudService;

    @PostMapping("/event")
    public ResponseEntity<FraudResponseDTO> registerFraudEvent(@RequestBody FraudEventDTO eventDTO) {
        log.info("📡 Réception événement fraude: {}", eventDTO.getEventType());
        return ResponseEntity.ok(fraudService.registerFraudEvent(eventDTO));
    }

    @GetMapping("/blocked/{examenId}/{employeId}")
    public ResponseEntity<Map<String, Object>> isStudentBlocked(
            @PathVariable String examenId,
            @PathVariable String employeId) {
        log.info("🔒 Vérification blocage - Examen: {}, Employé: {}", examenId, employeId);
        return ResponseEntity.ok(fraudService.getBlockedStatus(examenId, employeId));
    }

    @GetMapping("/report/{examenId}")
    public ResponseEntity<List<FraudSummaryDTO>> getExamFraudReport(@PathVariable String examenId) {
        return ResponseEntity.ok(fraudService.getExamFraudSummary(examenId));
    }
}
