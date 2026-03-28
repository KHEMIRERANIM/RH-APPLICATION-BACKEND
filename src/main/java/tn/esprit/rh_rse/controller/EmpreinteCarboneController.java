package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.EmpreinteCarbone;
import tn.esprit.rh_rse.service.EmpreinteCarboneService;

import java.util.List;

@RestController
@RequestMapping("/api/empreintes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class EmpreinteCarboneController {

    private final EmpreinteCarboneService empreinteCarboneService;

    @GetMapping
    public ResponseEntity<List<EmpreinteCarbone>> getAll() {
        return ResponseEntity.ok(empreinteCarboneService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpreinteCarbone> getById(
            @PathVariable String id) {
        return ResponseEntity.ok(empreinteCarboneService.getById(id));
    }

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<EmpreinteCarbone>> getByEmploye(
            @PathVariable String employeId) {
        return ResponseEntity.ok(empreinteCarboneService.getByEmployeId(employeId));
    }

    @GetMapping("/trajet/{trajetId}")
    public ResponseEntity<List<EmpreinteCarbone>> getByTrajet(
            @PathVariable String trajetId) {
        return ResponseEntity.ok(empreinteCarboneService.getByTrajetId(trajetId));
    }

    @GetMapping("/employe/{employeId}/points")
    public ResponseEntity<Integer> getTotalPoints(
            @PathVariable String employeId) {
        return ResponseEntity.ok(
                empreinteCarboneService.getTotalPointsByEmploye(employeId));
    }

    @PostMapping
    public ResponseEntity<EmpreinteCarbone> create(
            @RequestBody EmpreinteCarbone empreinte) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(empreinteCarboneService.create(empreinte));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpreinteCarbone> update(
            @PathVariable String id,
            @RequestBody EmpreinteCarbone empreinte) {
        return ResponseEntity.ok(empreinteCarboneService.update(id, empreinte));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        empreinteCarboneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}