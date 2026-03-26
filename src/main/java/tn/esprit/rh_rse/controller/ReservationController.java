package tn.esprit.rh_rse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.config.jwt.JwtUtil;
import tn.esprit.rh_rse.entity.Reservation;
import tn.esprit.rh_rse.service.ReservationService;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Reservation> reserverOuModifier(
            @RequestParam String idOffre,
            @RequestParam Integer nbPersonnes,
            HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        return ResponseEntity.ok(reservationService.reserverOuModifier(idUser, idOffre, nbPersonnes));
    }

    @GetMapping("/mes-reservations")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<List<Reservation>> getMesReservations(HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        return ResponseEntity.ok(reservationService.getMesReservations(idUser));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Reservation> annuler(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        return ResponseEntity.ok(reservationService.annuler(idUser, id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Reservation>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping("/offre/{idOffre}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Reservation>> getByOffre(@PathVariable String idOffre) {
        return ResponseEntity.ok(reservationService.getByOffre(idOffre));
    }

    private String extraireIdUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.extractUserId(token);
        }
        throw new RuntimeException("Token non trouvé");
    }
}