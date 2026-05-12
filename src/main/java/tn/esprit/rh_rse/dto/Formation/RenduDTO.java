package tn.esprit.rh_rse.dto.Formation;

import lombok.Data;

import java.util.Date;

@Data
public class RenduDTO {
    private String id;
    private String documentId;
    private String employeId;
    private String employeNom;
    private String fileName;
    private Date submittedAt;
    private Double note;
    private String statut;
}