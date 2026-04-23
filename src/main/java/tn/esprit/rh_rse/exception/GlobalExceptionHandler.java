package tn.esprit.rh_rse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailExists(EmailAlreadyExistsException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }
    // Ajoute cette méthode dans ton GlobalExceptionHandler.java existant
    @ExceptionHandler(RecrutementNotFoundException.class)
    public ResponseEntity<String> handleRecrutementNotFound(RecrutementNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // NOUVEAU
    @ExceptionHandler(OffreAvantageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOffreNotFound(OffreAvantageNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // NOUVEAU
    @ExceptionHandler(PartenaireNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePartenaireNotFound(PartenaireNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // NOUVEAU
    @ExceptionHandler(PlacesIndisponiblesException.class)
    public ResponseEntity<Map<String, Object>> handlePlacesIndisponibles(PlacesIndisponiblesException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        ex.printStackTrace(); // Log the exact error in IntelliJ
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", status.value());
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(error);
    }
}