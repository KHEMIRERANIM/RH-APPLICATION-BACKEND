package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "inscriptions_formation")
@Data
public class InscriptionFormation {

    @Id
    private String id;

    @Field("formation_id")
    private String formationId;

    @Field("employe_id")
    private String employeId;

    @Field("statut")
    private String statut; // EN_ATTENTE, CONFIRME, ANNULE, TERMINE

    @Field("date_inscription")
    private LocalDateTime dateInscription;

    @Field("motif_annulation")
    private String motifAnnulation;

    @Field("presence_confirmee")
    private Boolean presenceConfirmee = false;

    @Field("date_presence")
    private LocalDateTime datePresence;
}