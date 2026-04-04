package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.BusPackRequest;
import tn.esprit.rh_rse.entity.Bus;
import tn.esprit.rh_rse.service.BusService;

import java.util.List;

@RestController
@RequestMapping("/api/bus")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class BusController {

    private final BusService busService;

    @GetMapping
    public ResponseEntity<List<Bus>> getAll() {
        return ResponseEntity.ok(busService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bus> getById(@PathVariable String id) {
        return ResponseEntity.ok(busService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Bus> create(@RequestBody Bus bus) {
        return ResponseEntity.status(HttpStatus.CREATED).body(busService.create(bus));
    }

    @PostMapping("/pack")
    public ResponseEntity<List<Bus>> createPack(@RequestBody BusPackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(busService.createPack(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bus> update(@PathVariable String id, @RequestBody Bus bus) {
        return ResponseEntity.ok(busService.update(id, bus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        busService.delete(id);
        return ResponseEntity.noContent().build();
    }
}