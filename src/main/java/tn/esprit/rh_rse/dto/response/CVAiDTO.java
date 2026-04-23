package tn.esprit.rh_rse.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CVAiDTO {
    private ProfileDTO profile;
    private List<String> missingFields;
    private Map<String, Double> confidence;

    @Data
    public static class ProfileDTO {
        private String nomComplet;
        private String email;
        private String telephone;
        private String adresse;
        private Integer anneesExperience;
        private List<String> skills;
        private List<String> languages;
    }
}
