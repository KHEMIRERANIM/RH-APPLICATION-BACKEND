package tn.esprit.rh_rse.entity.enums;

public enum TypeNotification {
    RESERVATION,          // réservation créée / confirmée / annulée
    DEMANDE_CONFIRMATION, // conducteur doit confirmer le passager
    MESSAGE,              // message entre membres du trajet
    CONFIRMATION_ALTERNATIVE,  // ← AJOUTER (confirmation alternative)
    ALTERNATIVES_DISPONIBLES   // ← AJOUTER (alternatives disponibles)
}