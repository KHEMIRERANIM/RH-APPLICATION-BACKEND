package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Avis;

import java.util.List;

public interface AvisRepository extends MongoRepository<Avis, String> {
    List<Avis> findByPlatId(String platId);
    List<Avis> findByUserId(String userId);
    boolean existsByUserIdAndPlatId(String userId, String platId);
}