package tn.esprit.rh_rse.exception;

public class PlacesIndisponiblesException extends RuntimeException {
    public PlacesIndisponiblesException(int placesDisponibles) {
        super("Nombre de places insuffisant. Places disponibles : " + placesDisponibles);
    }
}