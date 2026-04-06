package tn.esprit.rh_rse.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Career;

import java.util.List;

public interface CareerRepository extends MongoRepository<Career, String>{
    List<Career> findByTitle(String title);
}
