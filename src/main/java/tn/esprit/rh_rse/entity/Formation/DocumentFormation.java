package tn.esprit.rh_rse.entity.Formation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
public class DocumentFormation {
    @Id
    private String id;
    private String formationId;
    private String formateurId;
    private String titre;
    private String description;
    private String type; // COURS, EXERCICE, EXAMEN, RESSOURCE
    private String fileName;
    private String fileType;
    private long fileSize;
    private String filePath;
    private Date uploadedAt;
    private boolean active;
    private boolean valide; // AJOUTER CE CHAMP

    public DocumentFormation(String formationId, String formateurId, String titre,
                             String description, String type, String fileName,
                             String fileType, long fileSize, String filePath) {
        this.formationId = formationId;
        this.formateurId = formateurId;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.uploadedAt = new Date();
        this.active = true;
        this.valide = true; // Par défaut, le document est valide
    }
}