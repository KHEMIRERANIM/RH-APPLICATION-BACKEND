package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.Wishlist;
import tn.esprit.rh_rse.entity.enums.CategorieOffre;
import tn.esprit.rh_rse.exception.OffreNotFoundException;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.repository.WishlistRepository;
import tn.esprit.rh_rse.service.WishlistService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final OffreRepository offreRepository;

    @Override
    public Wishlist ajouterFavori(String idUser, String idOffre) {
        // 1/ verifier si l'offre existe
        Offre offre = offreRepository.findById(idOffre)
                .orElseThrow(() -> new OffreNotFoundException(idOffre));

        // 2/ verifier si dzja en favori pour eviter les doublons (meme si gere par index, c'est plus propre)
        if (wishlistRepository.existsByIdUserAndIdOffre(idUser, idOffre)) {
            throw new RuntimeException("Cette offre est déjà dans vos favoris !");
        }

        // 3/determiner le prix actuel pour le snapshot
        Double prixSnapshot = 0.0;
        if (offre.getCategorie() == CategorieOffre.HOTEL && offre.getDetailsHotel() != null) {
            prixSnapshot = offre.getDetailsHotel().getPrixAdulte() != null ? offre.getDetailsHotel().getPrixAdulte() : 0.0;
        } else {
            prixSnapshot = offre.getPrixConvention() != null ? offre.getPrixConvention() : 0.0;
        }

        // 4/creer et sauvegarder le nouveau favori
        Wishlist wishlist = Wishlist.builder()
                .idUser(idUser)
                .idOffre(idOffre)
                .dateAjout(LocalDateTime.now())
                .dernierPrixConnu(prixSnapshot)
                .dernieresPlacesDispoConnues(offre.getNbPlacesDispo() != null ? offre.getNbPlacesDispo() : 0)
                .build();

        return wishlistRepository.save(wishlist);
    }

    @Override
    public void retirerFavori(String idUser, String idOffre) {
        wishlistRepository.deleteByIdUserAndIdOffre(idUser, idOffre);
    }

    @Override
    public List<Wishlist> getMesFavoris(String idUser) {
        return wishlistRepository.findByIdUser(idUser);
    }

    @Override
    public boolean estEnFavori(String idUser, String idOffre) {
        return wishlistRepository.existsByIdUserAndIdOffre(idUser, idOffre);
    }
}
