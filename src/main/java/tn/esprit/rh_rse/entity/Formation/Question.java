package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
public class Question {

    @Field("id")
    private String id;

    @Field("texte")
    private String texte;

    @Field("type")
    private String type; // QCM, TEXTE, CODE

    @Field("points")
    private Integer points = 1;

    @Field("options")
    private List<OptionQuestion> options; // Pour QCM

    @Field("correct_answer")
    private String correctAnswer; // Pour TEXTE et CODE

    @Field("code_template")
    private String codeTemplate; // Pour CODE
}