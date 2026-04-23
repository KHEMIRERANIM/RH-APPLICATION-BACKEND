package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "signatures_charte")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureCharte {

    @Id
    private String id;

    private String candidatureId;
    private String candidatId;
    private String nomComplet;
    private String email;

    private String signatureBase64; // Signature dessinée
    private boolean accepteCharte;
    private boolean accepteReglement;

    private String adresseIp;
    private LocalDateTime dateSigne;
    private boolean signe;
}