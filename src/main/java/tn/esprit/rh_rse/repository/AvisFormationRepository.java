package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.AvisFormation;

import java.util.List;

public interface AvisFormationRepository extends MongoRepository<AvisFormation, String> {
    List<AvisFormation> findByPlatId(String platId);
    List<AvisFormation> findByUserId(String userId);
    boolean existsByUserIdAndPlatId(String userId, String platId);
}