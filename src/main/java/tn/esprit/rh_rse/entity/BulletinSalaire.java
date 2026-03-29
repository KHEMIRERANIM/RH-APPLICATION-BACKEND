package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "bulletins_salaire")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinSalaire {

    @Id
    private String id;

    private String employeId;

    private int mois;    // 1-12
    private int annee;

    // Éléments de rémunération
    private double salaireBrut;
    private double primes;
    private double heuresSupplementaires;

    // Cotisations & retenues
    private double cotisationsCNSS;   // ~9.18% en Tunisie
    private double irpp;              // Impôt sur le revenu
    private double autresRetenues;

    // Résultat
    private double salaireNet;        // salaireBrut + primes + HS - cotisations - irpp - retenues

    // Métadonnées
    private LocalDateTime dateGeneration;
    private String pdfUrl;            // lien de téléchargement du bulletin PDF
}
