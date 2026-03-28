package tn.esprit.rh_rse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.config.jwt.JwtUtil;
import tn.esprit.rh_rse.entity.Reservation;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.service.ReservationService;
import tn.esprit.rh_rse.service.OffreService;
import tn.esprit.rh_rse.service.PdfGenerationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import tn.esprit.rh_rse.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final OffreService offreService;
    private final UserRepository userRepository;
    private final PdfGenerationService pdfGenerationService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Reservation> reserverOuModifier(
            @RequestParam("idOffre") String idOffre,
            @RequestParam("nbPersonnes") Integer nbPersonnes,
            HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        return ResponseEntity.ok(reservationService.reserverOuModifier(idUser, idOffre, nbPersonnes));
    }

    /** Réservation spécifique aux offres hôtelières (adultes + enfants + formule pension) */
    @PostMapping("/hotel")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Reservation> reserverHotel(
            @RequestParam("idOffre")    String  idOffre,
            @RequestParam("nbAdultes")  Integer nbAdultes,
            @RequestParam("nbEnfants")  Integer nbEnfants,
            @RequestParam("formule")    String  formule,
            @RequestParam("checkIn") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate checkIn,
            @RequestParam("checkOut") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate checkOut,
            HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        return ResponseEntity.ok(reservationService.reserverHotel(idUser, idOffre, nbAdultes, nbEnfants, formule, checkIn, checkOut));
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
            @PathVariable("id") String id,
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
    public ResponseEntity<List<Reservation>> getByOffre(@PathVariable("idOffre") String idOffre) {
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

    @DeleteMapping("/mes-reservations/annulees")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Void> viderAnnulees(HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        reservationService.viderAnnulees(idUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<byte[]> telechargerPdf(
            @PathVariable("id") String id,
            HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        
        // Fetch to ensure ownership
        Reservation r = reservationService.getMesReservations(idUser).stream()
            .filter(res -> res.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Réservation non trouvée ou accès refusé."));
            
        Offre o = offreService.getById(r.getIdOffre());
        User u = userRepository.findById(idUser).orElseThrow(() -> new RuntimeException("User not found"));
        
        byte[] pdfContent = pdfGenerationService.generateReservationPdf(r, o, u);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("Reservation_" + r.getId() + ".pdf").build());
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}