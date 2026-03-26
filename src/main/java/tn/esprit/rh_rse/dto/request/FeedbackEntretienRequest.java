package tn.esprit.rh_rse.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class FeedbackEntretienRequest {
    private String feedbackGlobal;
    private Integer noteGlobale;
    private List<String> pointsForts;
    private List<String> pointsFaibles;
    private boolean recommandeEmbauche;
}