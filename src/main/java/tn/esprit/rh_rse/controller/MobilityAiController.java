package tn.esprit.rh_rse.controller;


import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.MobilityPredictionRequest;
import tn.esprit.rh_rse.dto.response.MobilityPredictionResponse;
import tn.esprit.rh_rse.service.MobilityAiService;

@RestController
@RequestMapping("/api/mobility-ai")
@CrossOrigin(origins = "http://localhost:4200")
public class MobilityAiController {

    private final MobilityAiService mobilityAiService;

    public MobilityAiController(MobilityAiService mobilityAiService) {
        this.mobilityAiService = mobilityAiService;
    }

    @PostMapping("/predict")
    public MobilityPredictionResponse predict(
            @RequestBody MobilityPredictionRequest request) {

        return mobilityAiService.predict(request);
    }
}