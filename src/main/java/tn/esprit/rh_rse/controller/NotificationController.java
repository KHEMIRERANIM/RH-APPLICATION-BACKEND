package tn.esprit.rh_rse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.service.NotificationService;
import tn.esprit.rh_rse.config.jwt.JwtUtil;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    private String extraireIdUser(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            String jwt = headerAuth.substring(7);
            return jwtUtil.extractUserId(jwt);
        }
        throw new RuntimeException("Utilisateur non authentifié");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<List<Notification>> getMesNotifications(HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        return ResponseEntity.ok(notificationService.getMesNotifications(idUser));
    }

    @PatchMapping("/{id}/lire")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<Notification> marquerCommeLu(@PathVariable String id, HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        return ResponseEntity.ok(notificationService.marquerCommeLu(id, idUser));
    }

    @PatchMapping("/tout-lire")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<Void> marquerToutCommeLu(HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        notificationService.marquerToutCommeLu(idUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<Long> getNbNonLues(HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        return ResponseEntity.ok(notificationService.getNbNonLues(idUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<Void> supprimerNotification(@PathVariable String id, HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        notificationService.supprimerNotification(id, idUser);
        return ResponseEntity.noContent().build();
    }
}
