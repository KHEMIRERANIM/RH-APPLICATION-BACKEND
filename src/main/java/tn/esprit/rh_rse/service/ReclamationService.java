package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Reclamation;
import java.util.List;

public interface ReclamationService {
    Reclamation saveReclamation(Reclamation reclamation);
    List<Reclamation> getAllReclamations();
}
