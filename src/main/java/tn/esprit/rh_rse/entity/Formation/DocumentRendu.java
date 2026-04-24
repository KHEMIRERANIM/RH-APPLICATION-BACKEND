package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
public class DocumentRendu {

    @Field("id")
    private String id;

    @Field("titre")
    private String titre;

    @Field("url")
    private String url;

    @Field("file_name")
    private String fileName;

    @Field("file_size")
    private Long fileSize;

    @Field("uploaded_at")
    private LocalDateTime uploadedAt;

    @Field("valide")
    private Boolean valide = false;

    @Field("commentaire")
    private String commentaire;
}