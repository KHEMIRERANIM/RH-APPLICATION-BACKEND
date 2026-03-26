package tn.esprit.rh_rse.exception;

public class OffreNotFoundException extends RuntimeException {
    public OffreNotFoundException(String id) {
        super("Offre non trouvée avec l'id : " + id);
    }
}