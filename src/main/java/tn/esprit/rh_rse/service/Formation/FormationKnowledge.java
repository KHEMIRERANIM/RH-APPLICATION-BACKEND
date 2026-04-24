// FormationKnowledge.java - Version complète
package tn.esprit.rh_rse.service.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormationKnowledge {
    private String id;
    private String titre;
    private String description;
    private String objectifs;
    private String preRequis;
    private String type;
    private String niveau;
    private String formateur;
    private String formateurBio;
    private Integer dureeHeures;
    private Integer nombrePlaces;
    private Integer placesDisponibles;
    private String lieu;
    private String lienVisio;
    private String lienGoogleMaps;
    private String imageUrl;
    private Boolean active;

    /**
     * Texte complet pour le contexte
     */
    public String getFullText() {
        StringBuilder text = new StringBuilder();
        text.append("Titre: ").append(titre != null ? titre : "").append("\n");
        text.append("Description: ").append(description != null ? description : "").append("\n");
        text.append("Objectifs: ").append(objectifs != null ? objectifs : "").append("\n");
        text.append("Prérequis: ").append(preRequis != null ? preRequis : "").append("\n");
        text.append("Type: ").append(type != null ? type : "").append("\n");
        text.append("Niveau: ").append(niveau != null ? niveau : "").append("\n");
        text.append("Formateur: ").append(formateur != null ? formateur : "").append("\n");
        text.append("Durée: ").append(dureeHeures != null ? dureeHeures + " heures" : "").append("\n");
        text.append("Lieu: ").append(lieu != null ? lieu : "").append("\n");
        return text.toString();
    }
}