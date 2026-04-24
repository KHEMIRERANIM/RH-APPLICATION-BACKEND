package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.rh_rse.entity.Formation.TransactionPoint;
import tn.esprit.rh_rse.service.Formation.PointFormationService;

import java.util.List;

// PointFormationController.java - Ajouter cet endpoint
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Slf4j
public class PointFormationController {

    private final PointFormationService pointFormationService;

    // ✅ Endpoint pour récupérer les points d'un employé
    @GetMapping("/user/{userId}")
    public ResponseEntity<Integer> getUserPoints(@PathVariable String userId) {
        log.info("📊 Récupération des points pour l'utilisateur: {}", userId);
        int solde = pointFormationService.getSoldePoints(userId);
        return ResponseEntity.ok(solde);
    }

    // ✅ Endpoint pour l'historique (optionnel)
    @GetMapping("/user/{userId}/historique")
    public ResponseEntity<List<TransactionPoint>> getHistorique(@PathVariable String userId) {
        return ResponseEntity.ok(pointFormationService.getHistoriquePoints(userId));
    }
}