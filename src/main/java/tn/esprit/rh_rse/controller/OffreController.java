package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.CreateOffreRequest;
import tn.esprit.rh_rse.dto.request.UpdateOffreRequest;
import tn.esprit.rh_rse.dto.response.OffreResponse;
import tn.esprit.rh_rse.entity.enums.TypeContrat;
import tn.esprit.rh_rse.service.OffreService;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/offres")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class OffreController {

    private final OffreService offreService;

    @PostMapping
    public ResponseEntity<OffreResponse> createOffre(
            @RequestBody CreateOffreRequest request,
            @RequestParam String createurId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offreService.createOffre(request, createurId));
    }

    @GetMapping
    public ResponseEntity<List<OffreResponse>> getOffres(
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) TypeContrat typeContrat,
            @RequestParam(required = false) String localisation,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(offreService.getOffresFiltered(departement, typeContrat, localisation, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OffreResponse> getOffreById(@PathVariable String id) {
        return ResponseEntity.ok(offreService.getOffreById(id));
    }

    @GetMapping("/admin/{createurId}")
    public ResponseEntity<List<OffreResponse>> getOffresByCreateur(@PathVariable String createurId) {
        return ResponseEntity.ok(offreService.getOffresByCreateurId(createurId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OffreResponse> updateOffre(
            @PathVariable String id,
            @RequestBody UpdateOffreRequest request) {
        return ResponseEntity.ok(offreService.updateOffre(id, request));
    }

    @PatchMapping("/{id}/publier")
    public ResponseEntity<Void> publierOffre(@PathVariable String id) {
        offreService.publierOffre(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/cloturer")
    public ResponseEntity<Void> cloturerOffre(@PathVariable String id) {
        offreService.cloturerOffre(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/archiver")
    public ResponseEntity<Void> archiverOffre(@PathVariable String id) {
        offreService.archiverOffre(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffre(@PathVariable String id) {
        offreService.deleteOffre(id);
        return ResponseEntity.noContent().build();
    }
}