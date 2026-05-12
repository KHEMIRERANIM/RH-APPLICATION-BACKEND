package tn.esprit.rh_rse.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.service.ReservationService;
import tn.esprit.rh_rse.service.StripeService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/paiement")
@CrossOrigin(origins = "http://localhost:4200")
public class PaiementController {

    private static final Logger log = LoggerFactory.getLogger(PaiementController.class);

    private final StripeService stripeService;
    private final ReservationService reservationService;

    public PaiementController(StripeService stripeService, ReservationService reservationService) {
        this.stripeService = stripeService;
        this.reservationService = reservationService;
    }

    @PostMapping("/stripe/create")
    public ResponseEntity<Map<String, String>> createStripePayment(@RequestBody Map<String, Object> body) {
        try {
            if (body.get("reservationId") == null || body.get("montant") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "reservationId ou montant manquant"));
            }
            String reservationId = body.get("reservationId").toString();
            Double montant = Double.valueOf(body.get("montant").toString());
            
            // Extract Stripe Token (PCI Compliant way)
            String stripeToken = body.get("stripeToken") != null ? body.get("stripeToken").toString() : null;

            PaymentIntent intent = stripeService.createPaymentIntent(reservationId, montant, stripeToken);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());
            response.put("status", intent.getStatus());

            if ("succeeded".equals(intent.getStatus())) {
                reservationService.confirmerReservationApresPaiement(reservationId);
            }
            
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            log.error("Erreur Stripe: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur lors de la création du paiement"));
        }
    }

    @PostMapping("/stripe/refund")
    public ResponseEntity<Map<String, String>> refundPayment(@RequestBody Map<String, Object> body) {
        try {
            String reservationId = body.get("reservationId").toString();
            stripeService.refundPayment(reservationId);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "refunded");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/stripe/success")
    public ResponseEntity<Void> stripeSuccess(@RequestParam("reservationId") String reservationId,
                                              @RequestParam("session_id") String sessionId) {
        try {
            log.info("Succès Stripe pour la réservation: {} (Session: {})", reservationId, sessionId);
            reservationService.confirmerReservationApresPaiement(reservationId);
            
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:4200/apps/covoiturage/user/dashboard?paymentSuccess=true")
                    .build();
        } catch (Exception e) {
            log.error("Erreur confirmation Stripe: {}", e.getMessage());
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:4200/apps/covoiturage/user/dashboard?paymentError=true")
                    .build();
        }
    }
}