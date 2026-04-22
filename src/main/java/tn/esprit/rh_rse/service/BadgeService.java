package tn.esprit.rh_rse.service;

import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.User;

import java.util.ArrayList;
import java.util.List;

@Service
public class BadgeService {

    public List<String> assignBadges(User user) {
        List<String> badges = new ArrayList<>();
        int points = user.getRsePoints();

        if (points >= 50) {
            badges.add("Green Starter");
        }
        if (points >= 150) {
            badges.add("Eco Warrior");
        }
        if (points >= 300) {
            badges.add("Green Champion");
        }

        return badges;
    }
}