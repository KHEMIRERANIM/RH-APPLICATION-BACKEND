package tn.esprit.rh_rse.controller;

import tn.esprit.rh_rse.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prediction")
@CrossOrigin(origins = "http://localhost:4200")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @GetMapping("/week")
    public String getWeeklyPrediction() {
        return predictionService.getWeeklyPrediction();
    }
}