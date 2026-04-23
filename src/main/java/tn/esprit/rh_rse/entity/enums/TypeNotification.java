package tn.esprit.rh_rse.entity.enums;

public enum TypeNotification {
    PLACES_LIBEREES,
    PRIX_BAISSE,
    EXPIRATION_PROCHE,
    RESERVATION,          // réservation créée / confirmée / annulée
    DEMANDE_CONFIRMATION, // conducteur doit confirmer le passager
    MESSAGE,              // message entre membres du trajet
    CONFIRMATION_ALTERNATIVE,  // À AJOUTER (confirmation alternative)
    ALTERNATIVES_DISPONIBLES,   // À AJOUTER (alternatives disponibles)
    /** Admin : assez d'employés en attente pour justifier d'activer un bus de réserve. */
    ACTIVATION_BUS
}
