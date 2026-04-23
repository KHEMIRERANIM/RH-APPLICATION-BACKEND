package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.SignatureCharte;
import tn.esprit.rh_rse.repository.SignatureCharteRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/recrutement/charte")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CharteController {

    private final SignatureCharteRepository signatureCharteRepository;

    // Vérifie si la charte est déjà signée
    @GetMapping("/{candidatureId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @PathVariable String candidatureId) {

        boolean dejaSignee = signatureCharteRepository
                .existsByCandidatureId(candidatureId);

        return ResponseEntity.ok(Map.of(
                "candidatureId", candidatureId,
                "signee", dejaSignee
        ));
    }

    // Enregistre la signature
    @PostMapping("/{candidatureId}/signer")
    public ResponseEntity<SignatureCharte> signerCharte(
            @PathVariable String candidatureId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        // Vérifie si déjà signé
        if (signatureCharteRepository.existsByCandidatureId(candidatureId)) {
            return ResponseEntity.badRequest().build();
        }

        SignatureCharte signature = SignatureCharte.builder()
                .candidatureId(candidatureId)
                .nomComplet((String) body.get("nomComplet"))
                .email((String) body.get("email"))
                .signatureBase64((String) body.get("signatureBase64"))
                .accepteCharte(Boolean.TRUE.equals(body.get("accepteCharte")))
                .accepteReglement(Boolean.TRUE.equals(body.get("accepteReglement")))
                .adresseIp(request.getRemoteAddr())
                .dateSigne(LocalDateTime.now())
                .signe(true)
                .build();

        return ResponseEntity.ok(signatureCharteRepository.save(signature));
    }

    // Récupère la signature
    @GetMapping("/{candidatureId}/signature")
    public ResponseEntity<SignatureCharte> getSignature(
            @PathVariable String candidatureId) {
        return signatureCharteRepository.findByCandidatureId(candidatureId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}