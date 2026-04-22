package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.RsePointTransaction;
import tn.esprit.rh_rse.entity.User;

import java.util.List;

@Repository
public interface RsePointTransactionRepository extends MongoRepository<RsePointTransaction, String> {

    List<RsePointTransaction> findByUser(User user);

    long countByUser(User user);
}