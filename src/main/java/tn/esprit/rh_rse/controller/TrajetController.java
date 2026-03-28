package tn.esprit.rh_rse.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Trajet;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.service.TrajetService;

import java.util.List;

@RestController
@RequestMapping("/api/trajets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class TrajetController {

    private final TrajetService trajetService;

    @GetMapping
    public List<Trajet> getAll() {
        return trajetService.getAll();
    }

    @GetMapping("/{id}")
    public Trajet getById(@PathVariable String id) {
        return trajetService.getById(id);
    }

    @GetMapping("/statut/{statut}")
    public List<Trajet> getByStatut(@PathVariable StatutTrajet statut) {
        return trajetService.getByStatut(statut);
    }

    @PostMapping
    public Trajet create(@RequestBody Trajet trajet) {
        return trajetService.create(trajet);
    }

    @PutMapping("/{id}")
    public Trajet update(@PathVariable String id, @RequestBody Trajet trajet) {
        return trajetService.update(id, trajet);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        trajetService.delete(id);
    }
}