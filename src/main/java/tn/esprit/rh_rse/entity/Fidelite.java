package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fidelite")
public class Fidelite {

    @Id
    private String id;

    private String userId;

    private int points = 0;

    private double totalDepense = 0.0;

    private boolean reductionDisponible = false;

    private double montantReduction = 5.0;

    private List<String> historique = new ArrayList<>();

    public static final int POINTS_PAR_TND = 10;
    public static final int SEUIL_REDUCTION = 500;
}
