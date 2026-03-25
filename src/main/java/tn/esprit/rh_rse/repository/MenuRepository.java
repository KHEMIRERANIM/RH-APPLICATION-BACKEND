package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Menu;

import java.time.LocalDate;
import java.util.List;

public interface MenuRepository extends MongoRepository<Menu, String> {
    List<Menu> findByStatut(String statut);
    List<Menu> findByDate(LocalDate date);
    boolean existsByDateAndStatut(LocalDate date, String statut);
}