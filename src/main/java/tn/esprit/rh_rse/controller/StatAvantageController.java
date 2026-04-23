package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.response.StatAvantageDTOs.*;
import tn.esprit.rh_rse.service.StatAvantageService;

import java.util.List;

@RestController
@RequestMapping("/api/avantages/stats")
@RequiredArgsConstructor
public class StatAvantageController {

    private final StatAvantageService statAvantageService;

    @GetMapping("/kpis")
    @PreAuthorize("hasRole('ADMIN')")
    public KpiDto getKpis() {
        return statAvantageService.getKpis();
    }

    @GetMapping("/par-categorie")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StatCategorieDto> getStatParCategorie() {
        return statAvantageService.getStatParCategorie();
    }

    @GetMapping("/top-offres")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StatTopOffreAvantageDto> getTopOffres() {
        return statAvantageService.getTop5Offres();
    }

    @GetMapping("/par-mois")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StatMensuelleDto> getStatParMois(@RequestParam(name = "annee", required = false) Integer annee) {
        return statAvantageService.getStatParMois(annee);
    }

    @GetMapping("/statuts")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StatStatutDto> getStatStatuts() {
        return statAvantageService.getStatStatuts();
    }
}
