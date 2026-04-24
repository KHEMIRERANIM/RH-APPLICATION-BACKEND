package tn.esprit.rh_rse.service;

import org.springframework.stereotype.Service;

@Service
public class RseService {

    public int calculatePoints(String actionType) {
        return switch (actionType) {
            case "ECO" -> 30;
            case "SOCIAL" -> 25;
            case "ETHICAL" -> 20;
            default -> 10;
        };
    }
}