package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "rendus_exercices")
public class RenduExercice {
    @Id
    private String id;
    private String documentId;
    private String employeId;
    private String employeNom;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String filePath;
    private Date submittedAt;
    private Double note;
    private String commentaire;
    private String statut; // EN_ATTENTE, NOTE, REJETE

    public RenduExercice() {
        this.submittedAt = new Date();
        this.statut = "EN_ATTENTE";
    }
}