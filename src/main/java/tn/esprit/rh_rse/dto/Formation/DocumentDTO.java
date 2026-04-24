package tn.esprit.rh_rse.dto.Formation;

import lombok.Data;

import java.util.Date;

@Data
public class DocumentDTO {
    private String id;
    private String formationId;
    private String formateurId;
    private String titre;
    private String description;
    private String type;
    private String fileName;
    private String fileType;
    private long fileSize;
    private Date uploadedAt;
}
