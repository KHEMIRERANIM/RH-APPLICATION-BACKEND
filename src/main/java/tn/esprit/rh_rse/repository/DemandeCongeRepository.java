package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.DemandeConge;
import tn.esprit.rh_rse.entity.enums.StatutConge;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DemandeCongeRepository extends MongoRepository<DemandeConge, String> {

    List<DemandeConge> findByEmployeId(String employeId);

    List<DemandeConge> findByManagerId(String managerId);

    List<DemandeConge> findByEmployeIdAndStatut(String employeId, StatutConge statut);

    List<DemandeConge> findByManagerIdAndStatut(String managerId, StatutConge statut);

    List<DemandeConge> findByEmployeIdAndDateDebutBetween(String employeId, LocalDate debut, LocalDate fin);

    List<DemandeConge> findByStatut(StatutConge statut);
    // Pour la détection de tendances : récupérer toutes les demandes approuvées d'un employé
    List<DemandeConge> findByEmployeIdAndStatut(String employeId, StatutConge statut, org.springframework.data.domain.Sort sort);
}
