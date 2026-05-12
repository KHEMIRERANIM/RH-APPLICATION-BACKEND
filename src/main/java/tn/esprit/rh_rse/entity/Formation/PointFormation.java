package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "points_formation")
@Data
public class PointFormation {

    @Id
    private String id;

    @Field("employe_id")
    private String employeId;

    @Field("employe_nom")
    private String employeNom;

    @Field("solde")
    private Integer solde = 1000;

    @Field("dernier_rechargement")
    private LocalDateTime dernierRechargement;

    @Field("historique")
    private List<TransactionPoint> historique = new ArrayList<>();

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}