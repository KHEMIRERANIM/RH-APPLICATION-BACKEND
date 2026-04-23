package tn.esprit.rh_rse.dto;


import lombok.Data;

@Data
public class RemplaceRequest {
    private String ancienneReservationId;
    private String nouvelleAlternativeId;
    private String typeAlternative;
    private String employeId;
}