package tn.esprit.rh_rse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StatAvantageDTOs {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class KpiDto {
        private long totalReservations;
        private long reservationsCeMois;
        private long offresActives;
        private long partenairesActifs;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StatCategorieDto {
        private String categorie;
        private long count;
        private double pourcentage;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StatTopOffreDto {
        private String idOffre;
        private String titreOffre;
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StatMensuelleDto {
        private int mois;
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StatStatutDto {
        private String statut;
        private long count;
        private double pourcentage;
    }
}
