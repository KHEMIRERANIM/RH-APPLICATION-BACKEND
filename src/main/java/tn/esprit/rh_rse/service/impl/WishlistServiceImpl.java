package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.OffreAvantage;
import tn.esprit.rh_rse.entity.Wishlist;
import tn.esprit.rh_rse.entity.enums.CategorieOffreAvantage;
import tn.esprit.rh_rse.exception.OffreAvantageNotFoundException;
import tn.esprit.rh_rse.repository.OffreAvantageRepository;
import tn.esprit.rh_rse.repository.WishlistRepository;
import tn.esprit.rh_rse.service.WishlistService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final OffreAvantageRepository offreAvantageRepository;

    @Override
    public Wishlist ajouterFavori(String idUser, String idOffreAvantage) {
        // 1/ verifier si l'offreAvantage existe
        OffreAvantage offreAvantage = offreAvantageRepository.findById(idOffreAvantage)
                .orElseThrow(() -> new OffreAvantageNotFoundException(idOffreAvantage));

        // 2/ verifier si dzja en favori pour eviter les doublons (meme si gere par index, c'est plus propre)
        if (wishlistRepository.existsByIdUserAndIdOffreAvantage(idUser, idOffreAvantage)) {
            throw new RuntimeException("Cette offre est déjà dans vos favoris !");
        }

        // 3/determiner le prix actuel pour le snapshot
        Double prixSnapshot = 0.0;
        if (offreAvantage.getCategorie() == CategorieOffreAvantage.HOTEL && offreAvantage.getDetailsHotel() != null) {
            prixSnapshot = offreAvantage.getDetailsHotel().getPrixAdulte() != null ? offreAvantage.getDetailsHotel().getPrixAdulte() : 0.0;
        } else {
            prixSnapshot = offreAvantage.getPrixConvention() != null ? offreAvantage.getPrixConvention() : 0.0;
        }

        // 4/creer et sauvegarder le nouveau favori
        Wishlist wishlist = Wishlist.builder()
                .idUser(idUser)
                .idOffreAvantage(idOffreAvantage)
                .dateAjout(LocalDateTime.now())
                .dernierPrixConnu(prixSnapshot)
                .dernieresPlacesDispoConnues(offreAvantage.getNbPlacesDispo() != null ? offreAvantage.getNbPlacesDispo() : 0)
                .build();

        return wishlistRepository.save(wishlist);
    }

    @Override
    public void retirerFavori(String idUser, String idOffreAvantage) {
        wishlistRepository.deleteByIdUserAndIdOffreAvantage(idUser, idOffreAvantage);
    }

    @Override
    public List<Wishlist> getMesFavoris(String idUser) {
        return wishlistRepository.findByIdUser(idUser);
    }

    @Override
    public boolean estEnFavori(String idUser, String idOffreAvantage) {
        return wishlistRepository.existsByIdUserAndIdOffreAvantage(idUser, idOffreAvantage);
    }
}
