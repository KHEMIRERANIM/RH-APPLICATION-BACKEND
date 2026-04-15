package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.response.MotivationAnalysisResponse;
import tn.esprit.rh_rse.entity.MobilityRequest;
import tn.esprit.rh_rse.service.MobilityRequestService;
import tn.esprit.rh_rse.service.MotivationAnalysisService;

@RestController
@RequestMapping("/api/mobility")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "http://localhost:4200") // ✅ fix CORS
public class MobilityAnalysisController {

    private final MotivationAnalysisService motivationAnalysisService;
    private final MobilityRequestService mobilityRequestService;

    @GetMapping("/{id}/analyze")
    public ResponseEntity<MotivationAnalysisResponse> analyzeMotivationLetter(
            @PathVariable String id) {

        // ✅ Récupérer les vraies données depuis MongoDB
        MobilityRequest request = mobilityRequestService.getById(id);

        if (request.getMotivationFileBase64() == null) {
            return ResponseEntity.badRequest().build();
        }

        MotivationAnalysisResponse result = motivationAnalysisService.analyze(
                request.getMotivationFileBase64(),
                request.getTargetCareerTitle()  != null ? request.getTargetCareerTitle()  : "",
                request.getCurrentCareerTitle() != null ? request.getCurrentCareerTitle() : "",
                request.getEmployeeName()       != null ? request.getEmployeeName()       : "Employe"
        );

        return ResponseEntity.ok(result);
    }
}