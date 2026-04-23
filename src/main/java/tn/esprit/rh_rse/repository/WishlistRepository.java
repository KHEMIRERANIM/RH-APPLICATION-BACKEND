package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Wishlist;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends MongoRepository<Wishlist, String> {

    /** tous les favoris d'un utilisateur */
    List<Wishlist> findByIdUser(String idUser);

    Optional<Wishlist> findByIdUserAndIdOffreAvantage(String idUser, String idOffreAvantage);

    void deleteByIdUserAndIdOffreAvantage(String idUser, String idOffreAvantage);

    boolean existsByIdUserAndIdOffreAvantage(String idUser, String idOffreAvantage);
}
