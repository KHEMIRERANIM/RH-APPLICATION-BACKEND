package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.ReservationNavetteRequest;
import tn.esprit.rh_rse.dto.response.ReservationNavetteResponse;
import tn.esprit.rh_rse.service.ReservationNavetteService;

import java.util.List;

@RestController
@RequestMapping("/api/reservations-navette")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReservationNavetteController {

    private final ReservationNavetteService reservationNavetteService;

    @GetMapping
    public ResponseEntity<List<ReservationNavetteResponse>> getAll() {
        return ResponseEntity.ok(reservationNavetteService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationNavetteResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(reservationNavetteService.getById(id));
    }

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<ReservationNavetteResponse>> getByEmployeId(@PathVariable String employeId) {
        return ResponseEntity.ok(reservationNavetteService.getByEmployeId(employeId));
    }

    @GetMapping("/employe/{employeId}/points")
    public ResponseEntity<Integer> getTotalPoints(@PathVariable String employeId) {
        return ResponseEntity.ok(reservationNavetteService.getTotalPointsByEmploye(employeId));
    }

    @PostMapping
    public ResponseEntity<ReservationNavetteResponse> create(@RequestBody ReservationNavetteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationNavetteService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationNavetteResponse> update(@PathVariable String id, @RequestBody ReservationNavetteRequest request) {
        return ResponseEntity.ok(reservationNavetteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        reservationNavetteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}