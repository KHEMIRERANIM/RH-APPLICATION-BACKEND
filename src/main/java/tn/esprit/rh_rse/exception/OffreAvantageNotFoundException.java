package tn.esprit.rh_rse.exception;

public class OffreAvantageNotFoundException extends RuntimeException {
    public OffreAvantageNotFoundException(String id) {
        super("OffreAvantage non trouvée avec l'id : " + id);
    }
}