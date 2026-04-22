package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.UserBadge;
import tn.esprit.rh_rse.entity.User;

import java.util.List;

@Repository
public interface UserBadgeRepository extends MongoRepository<UserBadge, String> {

    List<UserBadge> findByUser(User user);
}