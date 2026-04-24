package tn.esprit.rh_rse.entity.conge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "soldes_conge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldeConge {

    @Id
    private String id;

    private String employeId;

    private int annee;

    @Builder.Default
    private int joursTotal = 30;           // Droits annuels (ex: 30 jours)
    private int joursUtilises;
    private int joursEnAttente;            // Demandes EN_ATTENTE
    private int joursRestants;            // joursTotal - joursUtilises - joursEnAttente

    private LocalDateTime updatedAt;
}
