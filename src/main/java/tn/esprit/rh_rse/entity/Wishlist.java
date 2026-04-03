package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;


@Document(collection = "wishlist")
@CompoundIndex(def = "{'idUser': 1, 'idOffre': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @Id
    private String id;

    private String idUser;

    private String idOffre;

    private LocalDateTime dateAjout;

    private Double dernierPrixConnu;
}
