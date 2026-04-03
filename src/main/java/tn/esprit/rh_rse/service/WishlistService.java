package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Wishlist;

import java.util.List;

public interface WishlistService {

    Wishlist ajouterFavori(String idUser, String idOffre);

    void retirerFavori(String idUser, String idOffre);

    List<Wishlist> getMesFavoris(String idUser);

    boolean estEnFavori(String idUser, String idOffre);
}
