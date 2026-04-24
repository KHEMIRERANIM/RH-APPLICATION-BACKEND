package tn.esprit.rh_rse.dto.Formation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RapportIADTO {
    private String titre;
    private LocalDateTime dateGeneration;
    private String periode;
    private String resumeExecutif;
    private String analyseDetaillee;
    private String recommandations;
    private String previsions;
    private String version;
    private String statut;
}