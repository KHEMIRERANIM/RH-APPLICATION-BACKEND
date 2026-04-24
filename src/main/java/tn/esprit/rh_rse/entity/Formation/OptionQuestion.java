package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class OptionQuestion {

    @Field("id")
    private String id;

    @Field("texte")
    private String texte;

    @Field("est_correct")
    private Boolean estCorrect = false;
}