// entity/Formation/VoteProposition.java
package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "votes_proposition")
@Data
public class VoteProposition {

    @Id
    private String id;

    @Field("proposition_id")
    private String propositionId;

    @Field("employe_id")
    private String employeId;

    @Field("employe_nom")
    private String employeNom;

    @Field("employe_email")
    private String employeEmail;

    @Field("vote")
    private String vote; // POUR, CONTRE, PEUT_ETRE

    @Field("commentaire")
    private String commentaire;

    @Field("date_vote")
    @CreatedDate
    private LocalDateTime dateVote;
}