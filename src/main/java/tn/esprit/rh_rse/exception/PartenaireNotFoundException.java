package tn.esprit.rh_rse.exception;

public class PartenaireNotFoundException extends RuntimeException {
    public PartenaireNotFoundException(String id) {
        super("Partenaire non trouvé avec l'id : " + id);
    }
}