package tn.esprit.rh_rse.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.rh_rse.entity.Career;

public interface CareerRepository extends MongoRepository<Career, String>{
}
