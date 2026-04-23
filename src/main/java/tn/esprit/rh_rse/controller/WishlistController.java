package tn.esprit.rh_rse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.config.jwt.JwtUtil;
import tn.esprit.rh_rse.entity.Wishlist;
import tn.esprit.rh_rse.service.WishlistService;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Wishlist> ajouterFavori(
            @RequestParam("idOffreAvantage") String idOffreAvantage,
            HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        return ResponseEntity.ok(wishlistService.ajouterFavori(idUser, idOffreAvantage));
    }

    @DeleteMapping("/{idOffreAvantage}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Void> retirerFavori(
            @PathVariable("idOffreAvantage") String idOffreAvantage,
            HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        wishlistService.retirerFavori(idUser, idOffreAvantage);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mes-favoris")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<List<Wishlist>> getMesFavoris(HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        return ResponseEntity.ok(wishlistService.getMesFavoris(idUser));
    }

    @GetMapping("/check/{idOffreAvantage}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Boolean> estEnFavori(
            @PathVariable("idOffreAvantage") String idOffreAvantage,
            HttpServletRequest httpRequest) {
        String idUser = extraireIdUser(httpRequest);
        return ResponseEntity.ok(wishlistService.estEnFavori(idUser, idOffreAvantage));
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
