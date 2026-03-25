package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Commande;

import java.time.LocalDate;
import java.util.List;

public interface CommandeRepository extends MongoRepository<Commande, String> {
    List<Commande> findByUserId(String userId);
    List<Commande> findByStatut(String statut);
    List<Commande> findByMenuId(String menuId);
    List<Commande> findByUserIdAndDateCommande(String userId, LocalDate date);
    boolean existsByUserIdAndMenuId(String userId, String menuId);
}