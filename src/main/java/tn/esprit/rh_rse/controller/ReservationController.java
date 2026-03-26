package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.ReservationRequest;
import tn.esprit.rh_rse.dto.response.ReservationResponse;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import tn.esprit.rh_rse.service.ReservationService;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAll() {
        return ResponseEntity.ok(reservationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(
            @PathVariable String id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<ReservationResponse>> getByEmploye(
            @PathVariable String employeId) {
        return ResponseEntity.ok(reservationService.getByEmployeId(employeId));
    }

    @GetMapping("/trajet/{trajetId}")
    public ResponseEntity<List<ReservationResponse>> getByTrajet(
            @PathVariable String trajetId) {
        return ResponseEntity.ok(reservationService.getByTrajetId(trajetId));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<ReservationResponse>> getByStatut(
            @PathVariable StatutReservation statut) {
        return ResponseEntity.ok(reservationService.getByStatut(statut));
    }

    @GetMapping("/employe/{employeId}/points")
    public ResponseEntity<Integer> getTotalPoints(
            @PathVariable String employeId) {
        return ResponseEntity.ok(
                reservationService.getTotalPointsEcoByEmploye(employeId));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @RequestBody ReservationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable String id,
            @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}