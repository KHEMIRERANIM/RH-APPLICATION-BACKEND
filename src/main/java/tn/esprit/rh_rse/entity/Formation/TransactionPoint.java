package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionPoint {
    private String id;
    private String type; // "DEBIT", "CREDIT"
    private Integer montant;
    private String raison;
    private LocalDateTime date;
    private Integer soldeApres;
}