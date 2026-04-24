package tn.esprit.rh_rse.controller.Formation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.service.Formation.PointFormationService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "http://localhost:4200")
public class StripeController {

    private static final Logger log = LoggerFactory.getLogger(StripeController.class);
    private final PointFormationService pointService;

    public StripeController(PointFormationService pointService) {
        this.pointService = pointService;
    }

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> request) {
        try {
            int points = (int) request.get("points");
            int amount = (int) request.get("amount");
            String userId = (String) request.get("userId");

            log.info("💰 Demande d'achat: {} points pour {}€", points, amount);
            log.info("👤 Utilisateur: {}", userId);

            // Ajouter les points à l'utilisateur
            pointService.ajouterPoints(userId, points, "Achat de " + points + " points pour " + amount + "€");

            // Récupérer le nouveau solde
            int nouveauSolde = pointService.getSoldePoints(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", points + " points ajoutés avec succès !");
            response.put("points", points);
            response.put("nouveauSolde", nouveauSolde);

            log.info("✅ {} points ajoutés à l'utilisateur {}. Nouveau solde: {}", points, userId, nouveauSolde);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'achat de points: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}