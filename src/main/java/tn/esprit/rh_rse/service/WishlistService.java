package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Wishlist;

import java.util.List;

public interface WishlistService {

    Wishlist ajouterFavori(String idUser, String idOffreAvantage);

    void retirerFavori(String idUser, String idOffreAvantage);

    List<Wishlist> getMesFavoris(String idUser);

    boolean estEnFavori(String idUser, String idOffreAvantage);
}
