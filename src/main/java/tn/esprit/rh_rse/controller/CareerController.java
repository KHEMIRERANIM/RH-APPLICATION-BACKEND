package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Career;
import tn.esprit.rh_rse.service.CareerService;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CareerController {

    private final CareerService careerService;

    @PostMapping
    public ResponseEntity<Career> create(@RequestBody Career career) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(careerService.createCareer(career));
    }

    @GetMapping
    public ResponseEntity<List<Career>> getAll() {
        return ResponseEntity.ok(careerService.getAllCareers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Career> getById(@PathVariable String id) {
        return ResponseEntity.ok(careerService.getCareerById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Career> update(
            @PathVariable String id,
            @RequestBody Career career) {
        return ResponseEntity.ok(careerService.updateCareer(id, career));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        careerService.deleteCareer(id);
        return ResponseEntity.noContent().build();
    }
}