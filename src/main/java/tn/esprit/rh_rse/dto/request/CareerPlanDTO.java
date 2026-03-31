package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CareerPlanDTO {
    private String currentCareerId;
    private List<String> currentSkills;
    private String targetCareerId;
    private String notes;
}