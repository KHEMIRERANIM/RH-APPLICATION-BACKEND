package tn.esprit.rh_rse.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Commande;
import tn.esprit.rh_rse.entity.PaiementPlat;
import tn.esprit.rh_rse.service.CommandeService;
import tn.esprit.rh_rse.service.PaiementPlatService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commandes")
@CrossOrigin(origins = "http://localhost:4200")
@Validated
@RequiredArgsConstructor
public class CommandeController {
    private final CommandeService commandeService;
    private final PaiementPlatService paiementPlatService;

    @GetMapping
    public ResponseEntity<List<Commande>> getAll() {
        return ResponseEntity.ok(commandeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Commande> getById(@PathVariable String id) {
        return ResponseEntity.ok(commandeService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Commande>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(commandeService.getByUser(userId));
    }

    @PostMapping
    public ResponseEntity<Commande> create(@Valid @RequestBody Commande commande) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commandeService.save(commande));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<Commande> updateStatut(
            @PathVariable String id,
            @RequestParam String statut) {
        if (!statut.matches("en_attente|confirmee|prete|livree")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(commandeService.updateStatut(id, statut));
    }

    @PatchMapping("/{id}/plats")
    public ResponseEntity<Commande> updatePlats(
            @PathVariable String id,
            @RequestBody List<String> plats) {
        return ResponseEntity.ok(commandeService.updatePlats(id, plats));
    }

    @PostMapping("/{id}/payer")
    public ResponseEntity<PaiementPlat> payer(
            @PathVariable String id,
            @RequestParam String modePaiement) {
        if (!modePaiement.matches("especes|salaire")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(paiementPlatService.payerCommande(id, modePaiement));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        commandeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/jour")
    public ResponseEntity<Map<String, Long>> getNombreCommandesParJour() {
        return ResponseEntity.ok(commandeService.getNombreCommandesParJour());
    }

    @GetMapping("/stats/plats")
    public ResponseEntity<Map<String, Long>> getPlatsPlusCommandes() {
        return ResponseEntity.ok(commandeService.getPlatsPlusCommandes());
    }
}