package tn.esprit.rh_rse.entity.enums;

public enum StatutReservation {
    EN_ATTENTE,
    /** Liste d'attente : bus actif plein, en attente d'activation d'un bus du pack. */
    EN_ATTENTE_ACTIVATION,
    CONFIRME,
    ANNULE
}