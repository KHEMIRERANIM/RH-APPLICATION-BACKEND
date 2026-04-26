package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.service.impl.AITravelGuideService;

@RestController
@RequestMapping("/api/avantages/ia")
@RequiredArgsConstructor
public class AITravelController {

    private final AITravelGuideService aiTravelGuideService;

    @GetMapping("/programme")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<String> genererProgramme(@RequestParam("idOffre") String idOffre) {
        String jsonProgramme = aiTravelGuideService.genererProgramme(idOffre);
        return ResponseEntity.ok().header("Content-Type", "application/json").body(jsonProgramme);
    }
}
