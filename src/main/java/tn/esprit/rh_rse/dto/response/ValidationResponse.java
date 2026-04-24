package tn.esprit.rh_rse.dto.response;

import java.util.List;

public class ValidationResponse {

    private String userId;
    private boolean validated;
    private int pointsAdded;
    private int totalPoints;
    private String level;
    private List<String> badges;

    private String nom;
    private String prenom;
    private String email;

    public ValidationResponse(String userId,
                              boolean validated,
                              int pointsAdded,
                              int totalPoints,
                              String level,
                              List<String> badges,
                              String nom,
                              String prenom,
                              String email) {
        this.userId = userId;
        this.validated = validated;
        this.pointsAdded = pointsAdded;
        this.totalPoints = totalPoints;
        this.level = level;
        this.badges = badges;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isValidated() {
        return validated;
    }

    public int getPointsAdded() {
        return pointsAdded;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public String getLevel() {
        return level;
    }

    public List<String> getBadges() {
        return badges;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getEmail() {
        return email;
    }
}