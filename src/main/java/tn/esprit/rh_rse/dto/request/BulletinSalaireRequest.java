package tn.esprit.rh_rse.dto.request;

import lombok.Data;

@Data
public class BulletinSalaireRequest {
    private String employeId;
    private int mois;
    private int annee;
    private double salaireBrut;
    private double primes;
    private double heuresSupplementaires;
    private double autresRetenues;
}
