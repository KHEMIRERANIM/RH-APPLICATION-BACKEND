package tn.esprit.rh_rse.dto.request;

import lombok.Data;

@Data
public class MobilityPredictionRequest {
    private String currentJob;
    private double experienceYears;
    private double pythonYears;
    private double sqlYears;
    private double javaYears;
    private double angularYears;
    private double reactYears;
    private double cloudYears;
    private double devopsYears;
    private double securityYears;
    private double mlYears;
    private double testingYears;
}
