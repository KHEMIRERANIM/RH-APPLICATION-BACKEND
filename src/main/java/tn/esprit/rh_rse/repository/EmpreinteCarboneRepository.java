package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.EmpreinteCarbone;
import tn.esprit.rh_rse.entity.enums.RoleTrajet;

import java.util.List;

@Repository
public interface EmpreinteCarboneRepository extends MongoRepository<EmpreinteCarbone, String> {
    List<EmpreinteCarbone> findByEmployeId(String employeId);
    List<EmpreinteCarbone> findByTrajetId(String trajetId);
    List<EmpreinteCarbone> findByRole(RoleTrajet role);
}