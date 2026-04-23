package tn.esprit.rh_rse.controller;

import tn.esprit.rh_rse.dto.*;
import tn.esprit.rh_rse.dto.request.DemandeRemplacementCovoiturageRequest;
import tn.esprit.rh_rse.dto.response.ReservationResponse;
import tn.esprit.rh_rse.entity.Reservation;
import tn.esprit.rh_rse.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/alternatives")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AlternativeController {

    private final AlternativeTransportService alternativeService;
    private final RemplaceReservationService remplaceService;
    private final ReservationService reservationService;

    // Conducteur annule son trajet
    @PostMapping("/annuler-trajet/{trajetId}")
    public ResponseEntity<Void> annulerTrajet(
            @PathVariable String trajetId
    ) {
        alternativeService.annulerTrajetConducteur(trajetId);
        return ResponseEntity.ok().build();
    }

    // Récupérer alternatives
    @GetMapping("/annulation/{trajetId}")
    public ResponseEntity<List<AlternativeDTO>> getAlternatives(
            @PathVariable String trajetId,
            @RequestParam String employeId
    ) {
        return ResponseEntity.ok(
                alternativeService.findAlternatives(trajetId, employeId)
        );
    }

    // Choisir alternative (bus / navette : remplacement immédiat transactionnel)
    @PostMapping("/remplacer")
    public ResponseEntity<Reservation> remplacer(
            @RequestBody RemplaceRequest request
    ) {
        return ResponseEntity.ok(
                remplaceService.remplacerReservation(request)
        );
    }

    /** Covoiturage : demande au conducteur ; l'ancienne réservation est annulée seulement après confirmation */
    @PostMapping("/demander-remplacement-covoiturage")
    public ResponseEntity<ReservationResponse> demanderRemplacementCovoiturage(
            @RequestBody DemandeRemplacementCovoiturageRequest request
    ) {
        return ResponseEntity.ok(
                reservationService.demanderRemplacementCovoiturage(request)
        );
    }
}