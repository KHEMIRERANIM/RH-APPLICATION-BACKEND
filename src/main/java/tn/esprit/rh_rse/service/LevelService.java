package tn.esprit.rh_rse.service;

import org.springframework.stereotype.Service;

@Service
public class LevelService {

    public String getLevel(int points) {
        if (points < 50) return "Débutant";
        if (points < 150) return "Motivé";
        if (points < 300) return "Performer";
        if (points < 500) return "Expert";
        return "Champion";
    }
}