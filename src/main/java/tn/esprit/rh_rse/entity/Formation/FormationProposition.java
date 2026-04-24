// entity/Formation/FormationProposition.java
package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "formations_propositions")
@Data
public class FormationProposition {

    @Id
    private String id;

    @Field("technologie")
    private String technologie;

    @Field("titre")
    private String titre;

    @Field("description")
    private String description;

    @Field("objectifs")
    private String objectifs;

    @Field("duree_heures")
    private Integer dureeHeures;

    @Field("niveau")
    private String niveau; // DEBUTANT, INTERMEDIAIRE, EXPERT

    @Field("public_cible")
    private String publicCible;

    @Field("pre_requis")
    private String preRequis;

    @Field("type")
    private String type; // TECHNIQUE, MANAGERIAL, RSE, SOFT_SKILLS

    @Field("source")
    private String source; // "IA_RECOMMENDATION", "ADMIN_MANUAL"

    @Field("score_ia")
    private Double scoreIA; // Score de pertinence donné par l'IA

    @Field("statut")
    private String statut; // EN_ATTENTE_VALIDATION, VALIDEE, REJETEE, PROGRAMMEE

    @Field("commentaire_validation")
    private String commentaireValidation;

    @Field("employes_interesses")
    private List<InteretEmploye> employesInteresses = new ArrayList<>();

    @Field("date_proposition")
    @CreatedDate
    private LocalDateTime dateProposition;

    @Field("date_validation")
    private LocalDateTime dateValidation;

    @Field("valide_par")
    private String validePar;

    @Field("formation_creee_id")
    private String formationCreeeId;
    // Dans FormationProposition.java - AJOUTER CES CHAMPS

    @Field("votes")
    private List<VoteProposition> votes = new ArrayList<>();

    @Field("statut_vote")
    private String statutVote; // EN_COURS, CLOTURE, OUVERT

    @Field("date_fin_vote")
    private LocalDateTime dateFinVote;

    @Field("seuil_minimum")
    private Integer seuilMinimum = 5; // Nombre minimum de votes requis

    @Field("pourcentage_pour")
    private Double pourcentagePour; // Calculé automatiquement

    // Méthodes utilitaires
    public void ajouterVote(VoteProposition vote) {
        if (this.votes == null) {
            this.votes = new ArrayList<>();
        }
        // Supprimer l'ancien vote si existant
        this.votes.removeIf(v -> v.getEmployeId().equals(vote.getEmployeId()));
        this.votes.add(vote);
        calculerStatistiquesVotes();
    }

    public void calculerStatistiquesVotes() {
        if (votes == null || votes.isEmpty()) {
            this.pourcentagePour = 0.0;
            return;
        }
        long nbPour = votes.stream().filter(v -> "POUR".equals(v.getVote())).count();
        this.pourcentagePour = (nbPour * 100.0) / votes.size();
    }

    public int getNbVotesPour() {
        return votes == null ? 0 : (int) votes.stream().filter(v -> "POUR".equals(v.getVote())).count();
    }

    public int getNbVotesContre() {
        return votes == null ? 0 : (int) votes.stream().filter(v -> "CONTRE".equals(v.getVote())).count();
    }

    public int getNbVotesPeutEtre() {
        return votes == null ? 0 : (int) votes.stream().filter(v -> "PEUT_ETRE".equals(v.getVote())).count();
    }

    public int getTotalVotes() {
        return votes == null ? 0 : votes.size();
    }

    public boolean isVoteAtteint() {
        return getTotalVotes() >= seuilMinimum && pourcentagePour >= 50;
    }

    @Data
    public static class InteretEmploye {
        private String employeId;
        private String employeNom;
        private String employeEmail;
        private LocalDateTime dateInteret;
    }
}