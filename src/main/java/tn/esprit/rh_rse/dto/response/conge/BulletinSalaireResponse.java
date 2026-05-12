package tn.esprit.rh_rse.dto.response.conge;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BulletinSalaireResponse {
    private String id;
    private String employeId;
    private int mois;
    private int annee;
    private double salaireBrut;
    private double primes;
    private double heuresSupplementaires;
    private double cotisationsCNSS;
    private double irpp;
    private double autresRetenues;
    private double salaireNet;
    private LocalDateTime dateGeneration;
    private String pdfUrl;
}
