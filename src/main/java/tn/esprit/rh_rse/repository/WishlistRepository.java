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

    Optional<Wishlist> findByIdUserAndIdOffre(String idUser, String idOffre);

    void deleteByIdUserAndIdOffre(String idUser, String idOffre);

    boolean existsByIdUserAndIdOffre(String idUser, String idOffre);
}
